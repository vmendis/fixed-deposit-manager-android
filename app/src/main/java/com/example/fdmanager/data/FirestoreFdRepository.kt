package com.example.fdmanager.data

import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.SyncState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * #10 — users/{uid}/fds with offline persistence (CLOUD_GATE = cloud-1000r1).
 *
 * - Offline persistence is the Firestore Android DEFAULT (local cache + queued writes),
 *   so airplane-mode edits land in the cache instantly and sync on reconnect — nothing
 *   extra to enable.
 * - One snapshot listener (MetadataChanges.INCLUDE) is the single source of truth for
 *   [fds]: every write below becomes visible through the listener's next cache emission,
 *   so the UI needs no optimistic bookkeeping.
 * - The same listener drives the REAL #3 sync chip: pending writes → SYNCING, cache-only
 *   emission → OFFLINE, server-acknowledged → SAVED, listener failure → ERROR.
 * - `id` stays the client UUID (doc id), `createdAt`/`updatedAt` are server timestamps,
 *   `userId` is written per spec §3.2. Soft delete / recycle bin / renew semantics are
 *   IDENTICAL to the in-memory store ([renew]/[autoRenewDue] interface defaults).
 * - deleteForever REALLY deletes the document (owner decision 2026-11-04).
 * - Demo helpers are no-ops: demo data is local-only (owner decision 2026-11-04);
 *   [FdRepositoryProvider] routes those calls to the in-memory store instead.
 */
class FirestoreFdRepository(
    private val uid: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FdRepository {

    private val col = firestore.collection("users").document(uid).collection("fds")

    private val _fds = MutableStateFlow<List<FixedDeposit>>(emptyList())
    override val fds: StateFlow<List<FixedDeposit>> = _fds.asStateFlow()

    /** Real #3 chip input — null error unless the listener failed. */
    private val _syncState = MutableStateFlow(SyncState.SYNCING)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    /** Fires once after the first SERVER (not cache) snapshot — see [FdRepositoryProvider]. */
    @Volatile
    var onFirstServerSnapshot: (() -> Unit)? = null

    private var registration: ListenerRegistration? = null
    private var firstServerSnapshotSeen = false

    init {
        registration = col.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                _syncError.value = err.message ?: "Cloud listener failed"
                _syncState.value = SyncState.ERROR
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            _fds.value = snap.documents.mapNotNull { d ->
                d.data?.let { FdFirestoreMapper.fromMap(d.id, it) }
            }
            _syncError.value = null
            _syncState.value = when {
                snap.metadata.hasPendingWrites() -> SyncState.SYNCING
                snap.metadata.isFromCache -> SyncState.OFFLINE
                else -> SyncState.SAVED
            }
            if (!snap.metadata.isFromCache && !firstServerSnapshotSeen) {
                firstServerSnapshotSeen = true
                onFirstServerSnapshot?.invoke()
            }
        }
    }

    /** Called by the provider on sign-out/backend switch. The cache itself stays on disk. */
    fun close() {
        registration?.remove()
        registration = null
    }

    override fun add(fd: FixedDeposit): String {
        val id = if (fd.id.isBlank()) "fd-" + UUID.randomUUID().toString() else fd.id
        col.document(id).set(
            FdFirestoreMapper.toMap(fd.copy(id = id)) + mapOf(
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        return id
    }

    override fun update(fd: FixedDeposit) {
        col.document(fd.id).set(
            FdFirestoreMapper.toMap(fd) + mapOf(
                "userId" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge() // createdAt untouched
        )
    }

    override fun softDelete(id: String) {
        get(id)?.let { update(it.copy(isDeleted = true)) }
    }

    override fun restore(id: String) {
        get(id)?.let { update(it.copy(isDeleted = false)) }
    }

    override fun deleteForever(id: String) {
        col.document(id).delete()
    }

    override fun clearAll() {
        // #5 delete-all-data against the cloud: every doc, including the recycle bin.
        _fds.value.forEach { col.document(it.id).delete() }
    }

    /** Demo data is local-only — the provider never routes these here. Defensive no-ops. */
    override fun reset(sample: List<FixedDeposit>) = Unit
    override fun setDemoMode(enabled: Boolean, sample: List<FixedDeposit>) = Unit
}
