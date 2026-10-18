package com.example.fdmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vssbox.fdmanager.R
import com.example.fdmanager.domain.AuthValidation
import com.example.fdmanager.ui.FdViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    vm: FdViewModel,
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onForgot: () -> Unit,
    onSignedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("authBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("auth-0100r9"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.vssbox_cube),
                contentDescription = "vssbox logo",
                modifier = Modifier
                    .size(64.dp)
                    .testTag("vssboxLogoAuth")
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "vssbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("vssboxNameAuth")
            )
            Text(
                "FD Manager - Secure box",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null; generalError = null },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("emailField"),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null; generalError = null },
                label = { Text("Password") },
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("passwordField"),
                singleLine = true
            )
            generalError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("authError")
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val eErr = AuthValidation.validateEmail(email)
                    val pErr = AuthValidation.validatePassword(password)
                    emailError = eErr
                    passwordError = pErr
                    if (eErr == null && pErr == null) {
                        scope.launch {
                            val res = vm.mockSignIn(email, password)
                            if (res.isSuccess) {
                                snackbar.showSnackbar("Signed in as $email (mock)")
                                onSignedIn()
                            } else {
                                generalError = res.exceptionOrNull()?.message ?: "Sign in failed"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("authSubmit")
            ) {
                Text("Sign in")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onForgot, modifier = Modifier.testTag("forgotLink")) {
                    Text("Forgot password?")
                }
                TextButton(onClick = onSignUp, modifier = Modifier.testTag("signUpLink")) {
                    Text("Create account")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Mock session - no backend. Phase 2 will replace with Firebase Auth without layout changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    vm: FdViewModel,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignedUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("authBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.vssbox_cube),
                contentDescription = "vssbox logo",
                modifier = Modifier.size(64.dp).testTag("vssboxLogoAuth")
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "vssbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null; generalError = null },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("emailField"),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null; generalError = null },
                label = { Text("Password") },
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("passwordField"),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it; confirmError = null; generalError = null },
                label = { Text("Confirm password") },
                isError = confirmError != null,
                supportingText = { confirmError?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("confirmPasswordField"),
                singleLine = true
            )
            generalError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("authError")
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val eErr = AuthValidation.validateEmail(email)
                    val pErr = AuthValidation.validatePassword(password)
                    val cErr = AuthValidation.validateConfirm(password, confirm)
                    emailError = eErr
                    passwordError = pErr
                    confirmError = cErr
                    if (eErr == null && pErr == null && cErr == null) {
                        scope.launch {
                            val res = vm.mockSignUp(email, password, confirm)
                            if (res.isSuccess) {
                                snackbar.showSnackbar("Account created $email (mock)")
                                onSignedUp()
                            } else {
                                generalError = res.exceptionOrNull()?.message ?: "Sign up failed"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("authSubmit")
            ) {
                Text("Create account")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onSignIn, modifier = Modifier.testTag("signInLink")) {
                    Text("Sign in")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    vm: FdViewModel,
    onBack: () -> Unit,
    onSent: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("authBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.vssbox_cube),
                contentDescription = "vssbox logo",
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Reset your password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your email - we'll mock-send a reset link (no backend).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null; generalError = null; sent = false },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("emailField"),
                singleLine = true
            )
            generalError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("authError")
                )
            }
            if (sent) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "If an account exists for $email, a reset link has been sent (mock).",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("resetSentMessage")
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val eErr = AuthValidation.validateEmail(email)
                    emailError = eErr
                    if (eErr == null) {
                        scope.launch {
                            val res = vm.mockSendReset(email)
                            if (res.isSuccess) {
                                sent = true
                                snackbar.showSnackbar("Reset link sent to $email (mock)")
                            } else {
                                generalError = res.exceptionOrNull()?.message ?: "Failed to send"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("authSubmit")
            ) {
                Text("Send reset link")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSent, modifier = Modifier.fillMaxWidth().testTag("backToSignIn")) {
                Text("Back to sign in")
            }
        }
    }
}
