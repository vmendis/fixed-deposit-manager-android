package com.example.fdmanager.data

import android.content.Context
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.SyncState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * #10 — owns WHICH FdRepository backend is live and hides the switch behind [facade]
 * (CLOUD_GATE = cloud-1000r1).
 *
 * Selection rule (mirrors AuthRepositoryProvider, the pattern #9 proved):
 * - [forceFakeForTests] or instrumentation detected → in-memory, always. ALL Tier2 classes
 *   are protected without per-class boilerplate: zero Firestore traffic under test.
 * - Real auth backend + signed in → [FirestoreFdRepository] for that uid.
 * - Everything else (CI, fresh clones, signed-out, fake-auth sessions) → in-memory,
 *   exactly the pre-#10 behaviour.
 *
 * Owner decisions locked 2026-11-04:
 * - Migration on first sign-in copies USER-ENTERED FDs only (SampleData.SAMPLE_IDS never
 *   leave the device) and then removes them locally — a move, not a copy, so signed-out
 *   sessions can't show stale duplicates.
 * - Signed-out sessions keep showing the local store (demo or empty), as before #10.
 * - Demo/reset helpers ALWAYS act on the local store ([FacadeRepository] routes them).
 */
object FdRepositoryProvider {

    @Volatile
    var forceFakeForTests: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The pre-#10 store, created with the same opt-in demo seeding rule as before. */
    val inMemory: InMemoryFdRepository by lazy {
        val demoOn = try {
            SettingsStore.state.value.isDemoMode
        } catch (_: Exception) {
            false
        }
        InMemoryFdRepository(if (demoOn) SampleData.seed(LocalDate.now()) else emptyList())
    }

    private var cloud: FirestoreFdRepository? = null

    /** Real #3 chip override — null whenever the in-memory backend is active. */
    private val _cloudSyncState = MutableStateFlow<SyncState?>(null)
    val cloudSyncState: StateFlow<SyncState?> = _cloudSyncState.asStateFlow()
    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    val isCloudActive: Boolean get() = cloud != null

    private var started = false

    /** Call once from MainActivity.onCreate, AFTER AuthRepositoryProvider.init. */
    fun init(context: Context) {
        if (started) return
        started = true
        if (forceFakeForTests || runningUnderInstrumentation()) {
            forceFakeForTests = true
            return // facade stays on the in-memory store forever; no auth observation
        }
        scope.launch {
            AuthRepositoryProvider.get().authState.collect { auth ->
                val uid = if (!AuthRepositoryProvider.isUsingFake && auth.isLoggedIn) {
                    FirebaseAuth.getInstance().currentUser?.uid
                } else null
                if (uid != null) activateCloud(uid) else activateLocal()
            }
        }
    }

    @Synchronized
    private fun activateCloud(uid: String) {
        if (cloud != null) return
        val repo = FirestoreFdRepository(uid)
        cloud = repo
        migrateIfNeeded(uid, repo)
        // Issue #25 sweep for cloud data: only after the SERVER's truth has arrived, so a
        // stale cache can never double-renew (parents already RENEWED are filtered out).
        repo.onFirstServerSnapshot = { repo.autoRenewDue() }
        facadeImpl.switchTo(repo)
        mirrorSyncState(repo)
    }

    @Synchronized
    private fun activateLocal() {
        val old = cloud ?: return run { facadeImpl.switchTo(inMemory) }
        cloud = null
        syncMirrorJob?.cancel()
        _cloudSyncState.value = null
        _cloudSyncError.value = null
        old.close()
        facadeImpl.switchTo(inMemory)
    }

    private var syncMirrorJob: Job? = null
    private fun mirrorSyncState(repo: FirestoreFdRepository) {
        syncMirrorJob?.cancel()
        syncMirrorJob = scope.launch {
            launch { repo.syncState.collect { _cloudSyncState.value = it } }
            launch { repo.syncError.collect { _cloudSyncError.value = it } }
        }
    }

    /**
     * One-shot move of user-entered local FDs into the cloud, once per uid per device.
     * Offline-safe: the writes queue in the Firestore cache and sync on reconnect.
     */
    private fun migrateIfNeeded(uid: String, repo: FirestoreFdRepository) {
        if (DevicePrefs.isCloudMigrated(uid)) return
        val userFds = inMemory.fds.value.filter { it.id !in SampleData.SAMPLE_IDS }
        userFds.forEach { fd ->
            repo.add(fd)
            inMemory.deleteForever(fd.id)
        }
        DevicePrefs.setCloudMigrated(uid)
    }

    /**
     * Tier2 test APKs load into the app process, so their classes are visible here.
     * We probe for OUR OWN androidTest class — it exists ONLY in the test APK, never in
     * any app build, so this cannot misfire on a normal debug run. The guard means NO
     * Tier2 class can ever reach Firestore, even on a machine where google-services.json
     * and a signed-in emulator session exist.
     */
    private fun runningUnderInstrumentation(): Boolean = try {
        Class.forName("com.example.fdmanager.AuthFlowTest")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    private val facadeImpl: FacadeRepository by lazy { FacadeRepository() }

    /** What FdRepository.get() hands out — a stable object delegating to the live backend. */
    val facade: FdRepository get() = facadeImpl

    /**
     * Stable identity + stable [fds] StateFlow across backend switches, so ViewModels that
     * captured the repository once keep working when the user signs in or out.
     */
    private class FacadeRepository : FdRepository {

        @Volatile
        private var backend: FdRepository = FdRepositoryProvider.inMemory

        private val _fds = MutableStateFlow(backend.fds.value)
        override val fds: StateFlow<List<FixedDeposit>> = _fds.asStateFlow()

        private var mirrorJob: Job? = null

        init {
            switchTo(backend)
        }

        @Synchronized
        fun switchTo(newBackend: FdRepository) {
            backend = newBackend
            mirrorJob?.cancel()
            // Unconfined: each backend emission lands in _fds SYNCHRONOUSLY on the emitting
            // thread, so add-then-read flows (and Tier2 assertions) never see a stale facade.
            mirrorJob = FdRepositoryProvider.scope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
                newBackend.fds.collect { _fds.value = it }
            }
        }

        override fun add(fd: FixedDeposit): String = backend.add(fd)
        override fun update(fd: FixedDeposit) = backend.update(fd)
        override fun softDelete(id: String) = backend.softDelete(id)
        override fun restore(id: String) = backend.restore(id)
        override fun deleteForever(id: String) = backend.deleteForever(id)
        override fun renew(id: String, option: com.example.fdmanager.data.model.RenewOption?): String =
            backend.renew(id, option)
        override fun autoRenewDue(today: LocalDate): List<String> = backend.autoRenewDue(today)
        override fun clearAll() = backend.clearAll()

        // Demo data is LOCAL-ONLY (owner decision) — never routed to the cloud backend.
        override fun reset(sample: List<FixedDeposit>) = FdRepositoryProvider.inMemory.reset(sample)
        override fun setDemoMode(enabled: Boolean, sample: List<FixedDeposit>) =
            FdRepositoryProvider.inMemory.setDemoMode(enabled, sample)
    }
}
