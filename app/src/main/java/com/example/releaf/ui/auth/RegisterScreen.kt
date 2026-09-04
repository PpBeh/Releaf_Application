package com.example.releaf.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.releaf.R
import com.example.releaf.ui.components.LeafLogo
import com.example.releaf.ui.theme.AppStrings
import com.example.releaf.ui.viewmodel.ThemeViewModel
import androidx.compose.foundation.layout.size

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    error: String?,
    onRegisterClick: (name: String, email: String, password: String) -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    val lang by themeViewModel.language.collectAsState()
    fun t(key: String) = AppStrings.get(key, lang)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val isFormValid = name.isNotBlank() && email.isNotBlank()
            && password.length >= 6 && password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLoginClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back to Login",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        LeafLogo(
            modifier = Modifier.size(90.dp),
            animated = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(t("create_account"), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(t("join_releaf"), style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = if (it.isBlank()) t("name_required") else null
                if (error != null) onClearError()
            },
            label = { Text(t("name_label")) },
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = if (it.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(it).matches()) t("invalid_email_fmt") else null
                if (error != null) onClearError()
            },
            label = { Text(t("email_label")) },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = when {
                    it.isNotEmpty() && it.length < 6 -> t("pw_too_short")
                    else -> null
                }
                if (error != null) onClearError()
            },
            label = { Text(t("password_label")) },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(id = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                if (error != null) onClearError()
            },
            label = { Text(t("confirm_password_label")) },
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            supportingText = {
                when {
                    confirmPassword.isNotEmpty() && confirmPassword != password ->
                        Text(t("pw_mismatch"), color = MaterialTheme.colorScheme.error)
                    confirmPassword.isNotEmpty() && password.length < 6 ->
                        Text(t("pw_too_short"), color = MaterialTheme.colorScheme.error)
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isBlank()) {
                    nameError = t("name_required")
                    return@Button
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = t("invalid_email_fmt")
                    return@Button
                }
                if (password.length < 6) {
                    passwordError = t("pw_too_short")
                    return@Button
                }
                if (password != confirmPassword) {
                    passwordError = t("pw_mismatch")
                    return@Button
                }
                onRegisterClick(name, email, password)
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(t("register_btn"))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("already_have_account"))
            TextButton(onClick = onLoginClick) {
                Text(t("login_btn"))
            }
        }
    }
}
