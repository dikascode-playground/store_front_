package com.ibi.storefront.app.navigation

import android.widget.Toast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ibi.storefront.R
import com.ibi.storefront.core.model.AuthState
import com.ibi.storefront.core.model.AppLanguage
import com.ibi.storefront.core.model.AppTheme
import com.ibi.storefront.core.model.Product
import com.ibi.storefront.core.model.ProductSort
import com.ibi.storefront.ui.theme.FavoriteRed
import com.ibi.storefront.feature.auth.LoginAction
import com.ibi.storefront.feature.auth.LoginViewModel
import com.ibi.storefront.feature.auth.SignUpAction
import com.ibi.storefront.feature.auth.SignUpViewModel
import com.ibi.storefront.feature.favorites.FavoritesViewModel
import com.ibi.storefront.feature.productdetails.ProductDetailsViewModel
import com.ibi.storefront.feature.producteditor.ProductEditorAction
import com.ibi.storefront.feature.producteditor.ProductEditorViewModel
import com.ibi.storefront.feature.products.ProductsSyncAction
import com.ibi.storefront.feature.products.ProductsViewModel
import com.ibi.storefront.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreFrontApp(
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isAuthenticated = appState.authState is AuthState.Authenticated
    val showBackButton = currentRoute == AppDestination.SignUp.route ||
        currentRoute == AppDestination.Favorites.route ||
        currentRoute == AppDestination.Settings.route ||
        currentRoute?.startsWith("product_details/") == true ||
        currentRoute?.startsWith("product_editor") == true

    LaunchedEffect(isAuthenticated, currentRoute) {
        val publicRoutes = setOf(AppDestination.Login.route, AppDestination.SignUp.route)
        val authenticatedRoutes = setOf(
            AppDestination.Products.route,
            AppDestination.Favorites.route,
            AppDestination.Settings.route,
        )
        val targetRoute = when {
            currentRoute == null -> null
            isAuthenticated && currentRoute in publicRoutes -> AppDestination.Products.route
            isAuthenticated && currentRoute !in authenticatedRoutes &&
                !currentRoute.startsWith("product_details/") &&
                !currentRoute.startsWith("product_editor") -> AppDestination.Products.route
            !isAuthenticated && currentRoute !in publicRoutes -> AppDestination.Login.route
            else -> null
        }
        if (targetRoute != null) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(id = R.string.back_action),
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = when (currentRoute) {
                                AppDestination.Login.route -> stringResource(id = R.string.login_title)
                                AppDestination.SignUp.route -> stringResource(id = R.string.sign_up_title)
                                AppDestination.Products.route -> stringResource(id = R.string.products_title)
                                AppDestination.Favorites.route -> stringResource(id = R.string.favorites_title)
                                AppDestination.ProductEditor.route -> stringResource(id = R.string.product_editor_title)
                                AppDestination.Settings.route -> stringResource(id = R.string.settings_title)
                                else -> stringResource(id = R.string.app_name)
                            },
                        )
                    },
                    actions = {
                        if (isAuthenticated) {
                            IconButton(onClick = appViewModel::logout) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = stringResource(id = R.string.logout_action),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (!appState.isReady) {
                LoadingScreen(modifier = Modifier.padding(innerPadding))
            } else {
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Login.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(AppDestination.Login.route) {
                        when (appState.authState) {
                            is AuthState.Locked -> LockedSessionRoute(
                                message = appState.biometricMessage,
                                onBiometricUnlock = appViewModel::requestBiometricUnlock,
                                onUsePassword = appViewModel::logout,
                            )
                            else -> LoginRoute(
                                onOpenSignUp = { navController.navigate(AppDestination.SignUp.route) },
                            )
                        }
                    }
                    composable(AppDestination.SignUp.route) {
                        SignUpRoute(
                            onBackToLogin = { navController.popBackStack() },
                        )
                    }
                    composable(AppDestination.Products.route) {
                        ProductsRoute(
                            onOpenFavorites = { navController.navigate(AppDestination.Favorites.route) },
                            onOpenDetails = { productId ->
                                navController.navigate(AppDestination.ProductDetails.createRoute(productId))
                            },
                            onOpenEditor = { navController.navigate(AppDestination.ProductEditor.createRoute()) },
                            onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                        )
                    }
                    composable(AppDestination.Favorites.route) {
                        FavoritesRoute(
                            onOpenDetails = { productId ->
                                navController.navigate(AppDestination.ProductDetails.createRoute(productId))
                            },
                        )
                    }
                    composable(
                        route = AppDestination.ProductDetails.route,
                        arguments = listOf(navArgument("productId") { type = NavType.LongType }),
                    ) {
                        ProductDetailsRoute(
                            onEdit = { productId ->
                                navController.navigate(AppDestination.ProductEditor.createRoute(productId))
                            },
                            onDeleted = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = AppDestination.ProductEditor.route,
                        arguments = listOf(
                            navArgument("productId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) {
                        ProductEditorRoute(
                            onSaved = { navController.popBackStack() },
                        )
                    }
                    composable(AppDestination.Settings.route) {
                        SettingsRoute(
                            biometricMessage = appState.biometricMessage,
                            onEnableBiometric = appViewModel::requestBiometricEnrollment,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedSessionRoute(
    message: String?,
    onBiometricUnlock: () -> Unit,
    onUsePassword: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = stringResource(id = R.string.biometric_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.biometric_locked_message),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Button(onClick = onBiometricUnlock) {
                Text(stringResource(id = R.string.biometric_unlock_action))
            }
        }
        item {
            TextButton(
                onClick = onUsePassword,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(id = R.string.biometric_fallback_action))
            }
        }
        if (message != null) {
            item {
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LoginRoute(
    onOpenSignUp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loginError = state.loginError

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_header_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.login_heading),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.login_supporting_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onAction(LoginAction.UsernameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.username_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = state.usernameError != null,
                supportingText = state.usernameError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onAction(LoginAction.PasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text(stringResource(id = R.string.password_label)) },
                singleLine = true,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onAction(LoginAction.Submit) },
                ),
                trailingIcon = {
                    TextButton(onClick = { viewModel.onAction(LoginAction.TogglePasswordVisibility) }) {
                        Text(
                            text = stringResource(
                                id = if (state.isPasswordVisible) {
                                    R.string.hide_password_action
                                } else {
                                    R.string.show_password_action
                                },
                            ),
                        )
                    }
                },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it) } },
            )
        }
        item {
            Button(
                onClick = { viewModel.onAction(LoginAction.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = stringResource(id = R.string.login_action))
            }
        }
        if (loginError != null) {
            item {
                Text(
                    text = loginError,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onOpenSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(text = stringResource(id = R.string.open_sign_up_action))
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.seeded_review_account_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.seeded_review_account_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpRoute(
    onBackToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signUpError = state.signUpError

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_header_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.sign_up_heading),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.sign_up_supporting_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(id = R.string.sign_up_form_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        item {
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onAction(SignUpAction.UsernameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.username_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = state.usernameError != null,
                supportingText = state.usernameError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onAction(SignUpAction.PasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text(stringResource(id = R.string.password_label)) },
                singleLine = true,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    TextButton(onClick = { viewModel.onAction(SignUpAction.TogglePasswordVisibility) }) {
                        Text(
                            text = stringResource(
                                id = if (state.isPasswordVisible) {
                                    R.string.hide_password_action
                                } else {
                                    R.string.show_password_action
                                },
                            ),
                        )
                    }
                },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onAction(SignUpAction.ConfirmPasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text(stringResource(id = R.string.confirm_password_label)) },
                singleLine = true,
                visualTransformation = if (state.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onAction(SignUpAction.Submit) },
                ),
                trailingIcon = {
                    TextButton(onClick = { viewModel.onAction(SignUpAction.ToggleConfirmPasswordVisibility) }) {
                        Text(
                            text = stringResource(
                                id = if (state.isConfirmPasswordVisible) {
                                    R.string.hide_password_action
                                } else {
                                    R.string.show_password_action
                                },
                            ),
                        )
                    }
                },
                isError = state.confirmPasswordError != null,
                supportingText = state.confirmPasswordError?.let { { Text(it) } },
            )
        }
        item {
            Button(
                onClick = { viewModel.onAction(SignUpAction.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = stringResource(id = R.string.sign_up_action))
            }
        }
        if (signUpError != null) {
            item {
                Text(
                    text = signUpError,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(text = stringResource(id = R.string.back_to_sign_in_action))
            }
        }
    }
}

@Composable
private fun ProductsRoute(
    onOpenFavorites: () -> Unit,
    onOpenDetails: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()
    val statusMessage = state.statusMessage
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3 ||
                (listState.firstVisibleItemIndex > 0 && listState.firstVisibleItemScrollOffset > 200)
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(id = R.string.reset_local_changes_title)) },
            text = { Text(stringResource(id = R.string.reset_local_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        viewModel.resetLocalChanges()
                    },
                ) {
                    Text(stringResource(id = R.string.confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel_action))
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.products_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(id = R.string.products_supporting_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onSearchChanged,
                    modifier = Modifier
                        .fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.search_products_label)) },
                    singleLine = true,
                )
            }
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SortChip(
                            label = stringResource(id = R.string.filter_all_categories),
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.onCategorySelected(null) },
                        )
                    }
                    items(state.categories) { category ->
                        SortChip(
                            label = category,
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                        )
                    }
                }
            }
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SortChip(
                            label = stringResource(id = R.string.sort_default),
                            selected = state.selectedSort == ProductSort.Default,
                            onClick = { viewModel.onSortSelected(ProductSort.Default) },
                        )
                    }
                    item {
                        SortChip(
                            label = stringResource(id = R.string.sort_price_low),
                            selected = state.selectedSort == ProductSort.PriceLowToHigh,
                            onClick = { viewModel.onSortSelected(ProductSort.PriceLowToHigh) },
                        )
                    }
                    item {
                        SortChip(
                            label = stringResource(id = R.string.sort_rating_high),
                            selected = state.selectedSort == ProductSort.RatingHighToLow,
                            onClick = { viewModel.onSortSelected(ProductSort.RatingHighToLow) },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onOpenEditor,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(id = R.string.add_product_action))
                    }
                    OutlinedButton(
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(id = R.string.favorites_title))
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.tools_section_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(id = R.string.settings_title))
                            }
                            FilledTonalButton(
                                onClick = viewModel::refresh,
                                enabled = !state.isRefreshing,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Text(stringResource(id = R.string.refresh_action))
                            }
                        }
                        FilledTonalButton(
                            onClick = { showResetConfirmation = true },
                            enabled = !state.isRefreshing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(stringResource(id = R.string.reset_local_changes_action))
                        }
                    }
                }
            }
            if (state.isRefreshing) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (state.activeSyncAction == ProductsSyncAction.ResetLocalChanges) {
                                        R.string.syncing_reset_message
                                    } else {
                                        R.string.syncing_refresh_message
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
            if (statusMessage != null) {
                item {
                    StatusBanner(
                        message = statusMessage,
                        isError = state.statusIsError,
                        onDismiss = viewModel::clearStatusMessage,
                    )
                }
            }
            if (products.loadState.refresh is LoadState.Loading) {
                item { LoadingScreen() }
            }
            if (products.loadState.refresh is LoadState.Error && products.itemCount == 0) {
                item {
                    PlaceholderScreen(
                        title = stringResource(id = R.string.products_error_title),
                        message = stringResource(id = R.string.products_error_message),
                    )
                }
            }
            items(products.itemCount) { index ->
                val product = products[index]
                if (product != null) {
                    ProductCard(
                        product = product,
                        onFavoriteToggle = {
                            val isAddingFavorite = !product.isFavorite
                            viewModel.onFavoriteToggle(product.id, isAddingFavorite)
                            if (isAddingFavorite) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.favorite_added_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onOpenDetails = { onOpenDetails(product.id) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(id = R.string.back_to_top_action),
                )
            }
        }
    }
}

@Composable
private fun FavoritesRoute(
    onOpenDetails: (Long) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites = viewModel.favorites.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(id = R.string.undo_action)
    val removedSuffix = stringResource(id = R.string.favorites_removed_message_suffix)
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.favorites_empty_state),
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = Int.MAX_VALUE,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (favorites.itemCount == 0 && favorites.loadState.refresh !is LoadState.Loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LottieAnimation(
                                composition = lottieComposition,
                                progress = { lottieProgress },
                                modifier = Modifier.height(180.dp),
                            )
                            PlaceholderScreen(
                                title = stringResource(id = R.string.favorites_empty_title),
                                message = buildString {
                                    append(stringResource(id = R.string.favorites_empty_message))
                                    append("\n\n")
                                    append(stringResource(id = R.string.favorites_empty_message_secondary))
                                },
                            )
                        }
                    }
                }
            }
            items(favorites.itemCount) { index ->
                val product = favorites[index]
                if (product != null) {
                    ProductCard(
                        product = product,
                        onFavoriteToggle = {
                            viewModel.onFavoriteToggle(product.id, false)
                            val removedMessage = "${product.title} $removedSuffix"
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = removedMessage,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    viewModel.onFavoriteToggle(product.id, true)
                                }
                            }
                        },
                        onOpenDetails = { onOpenDetails(product.id) },
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ProductDetailsRoute(
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ProductDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val product = state.product
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }

    if (product == null) {
        LoadingScreen()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = product.thumbnail.ifBlank { product.images.firstOrNull() },
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.product_price_format, product.price),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = R.string.product_details_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.product_details_overview_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.product_category_format, product.category),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(id = R.string.product_stock_format, product.stock),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(id = R.string.product_rating_format, product.rating),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.product_details_description_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val isAddingFavorite = !product.isFavorite
                        viewModel.toggleFavorite()
                        if (isAddingFavorite) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.favorite_added_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            id = if (product.isFavorite) {
                                R.string.remove_from_favorites_action
                            } else {
                                R.string.add_to_favorites_action
                            },
                        ),
                    )
                }
                OutlinedButton(
                    onClick = { onEdit(product.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(id = R.string.edit_product_action))
                }
                FilledTonalButton(
                    onClick = viewModel::deleteProduct,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(id = R.string.delete_product_action))
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onFavoriteToggle: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Card(
        onClick = onOpenDetails,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                AsyncImage(
                    model = product.thumbnail.ifBlank { product.images.firstOrNull() },
                    modifier = Modifier
                        .height(84.dp)
                        .fillMaxWidth(0.24f)
                        .clip(MaterialTheme.shapes.medium),
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(id = R.string.product_price_format, product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = R.string.product_card_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (product.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = stringResource(id = R.string.favorite_toggle_action, product.title),
                    tint = if (product.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorRoute(
    onSaved: () -> Unit,
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val submitError = state.submitError
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    var useCustomCategory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    LaunchedEffect(state.category, state.categories) {
        if (state.category.isNotBlank() && state.category !in state.categories) {
            useCustomCategory = true
        }
        if (state.categories.isEmpty()) {
            useCustomCategory = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(ProductEditorAction.TitleChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_title_label)) },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onAction(ProductEditorAction.DescriptionChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_description_label)) },
                minLines = 3,
            )
        }
        item {
            OutlinedTextField(
                value = state.price,
                onValueChange = { viewModel.onAction(ProductEditorAction.PriceChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_price_label)) },
                isError = state.priceError != null,
                supportingText = state.priceError?.let { { Text(it) } },
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (useCustomCategory || state.categories.isEmpty()) {
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = { viewModel.onAction(ProductEditorAction.CategoryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.editor_category_label)) },
                        isError = state.categoryError != null,
                        supportingText = state.categoryError?.let { { Text(it) } },
                    )
                    if (state.categories.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                useCustomCategory = false
                                categoriesExpanded = false
                            },
                        ) {
                            Text(stringResource(id = R.string.editor_choose_existing_category_action))
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = categoriesExpanded,
                        onExpandedChange = { categoriesExpanded = !categoriesExpanded },
                    ) {
                        OutlinedTextField(
                            value = state.category,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(id = R.string.editor_category_label)) },
                            isError = state.categoryError != null,
                            supportingText = state.categoryError?.let { { Text(it) } },
                            trailingIcon = {
                                TrailingIcon(expanded = categoriesExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = categoriesExpanded,
                            onDismissRequest = { categoriesExpanded = false },
                        ) {
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.onAction(ProductEditorAction.CategoryChanged(category))
                                        categoriesExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            useCustomCategory = true
                            categoriesExpanded = false
                        },
                    ) {
                        Text(stringResource(id = R.string.editor_use_custom_category_action))
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.brand,
                onValueChange = { viewModel.onAction(ProductEditorAction.BrandChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_brand_label)) },
            )
        }
        item {
            OutlinedTextField(
                value = state.stock,
                onValueChange = { viewModel.onAction(ProductEditorAction.StockChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_stock_label)) },
                isError = state.stockError != null,
                supportingText = state.stockError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = state.thumbnail,
                onValueChange = { viewModel.onAction(ProductEditorAction.ThumbnailChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.editor_thumbnail_label)) },
            )
        }
        if (submitError != null) {
            item {
                Text(
                    text = submitError,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Button(
                onClick = { viewModel.onAction(ProductEditorAction.Submit) },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        id = if (state.isEditMode) {
                            R.string.update_product_action
                        } else {
                            R.string.create_product_action
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun SettingsRoute(
    biometricMessage: String?,
    onEnableBiometric: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.theme_section_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.theme_section_supporting_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            SortChip(
                                label = stringResource(id = R.string.theme_light),
                                selected = state.theme == AppTheme.Light,
                                onClick = { viewModel.setTheme(AppTheme.Light) },
                            )
                        }
                        item {
                            SortChip(
                                label = stringResource(id = R.string.theme_dark),
                                selected = state.theme == AppTheme.Dark,
                                onClick = { viewModel.setTheme(AppTheme.Dark) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.language_section_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.language_runtime_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            SortChip(
                                label = stringResource(id = R.string.language_english),
                                selected = state.language == AppLanguage.English,
                                onClick = { viewModel.setLanguage(AppLanguage.English) },
                            )
                        }
                        item {
                            SortChip(
                                label = stringResource(id = R.string.language_hebrew),
                                selected = state.language == AppLanguage.Hebrew,
                                onClick = { viewModel.setLanguage(AppLanguage.Hebrew) },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.biometric_section_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            id = if (state.biometricEnabled) {
                                R.string.biometric_status_enabled
                            } else {
                                R.string.biometric_status_disabled
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            SortChip(
                                label = stringResource(id = R.string.biometric_enabled),
                                selected = state.biometricEnabled,
                                onClick = onEnableBiometric,
                            )
                        }
                        item {
                            SortChip(
                                label = stringResource(id = R.string.biometric_disabled),
                                selected = !state.biometricEnabled,
                                onClick = { viewModel.setBiometricEnabled(false) },
                            )
                        }
                    }
                    if (biometricMessage != null) {
                        Text(
                            text = biometricMessage,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dismiss_action))
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
