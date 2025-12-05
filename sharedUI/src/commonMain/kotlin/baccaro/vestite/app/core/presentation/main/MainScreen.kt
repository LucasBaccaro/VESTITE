package baccaro.vestite.app.core.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import baccaro.vestite.app.core.presentation.home.HomeScreen
import baccaro.vestite.app.core.presentation.navigation.Screen
import baccaro.vestite.app.features.aiGeneration.presentation.AIGenerationScreen
import baccaro.vestite.app.features.looks.presentation.LooksScreen
import baccaro.vestite.app.features.wardrobe.presentation.list.WardrobeListScreen

/**
 * BottomBar navigation item data class.
 * Defines the appearance and behavior of each tab in the BottomNavigationBar.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Main screen with BottomNavigationBar.
 *
 * This is the container for the main app sections accessible via bottom tabs:
 * - Home
 * - Wardrobe
 * - Looks
 * - AI Generation
 *
 * Navigation within this screen maintains the BottomBar visible.
 * Navigation to secondary screens (Profile, Upload, Chat) is handled by the parent NavHost.
 *
 * @param onNavigateToProfile Callback to navigate to profile screen
 * @param onNavigateToUpload Callback to navigate to upload garment screen
 * @param onNavigateToChatAssistant Callback to navigate to chat assistant screen
 * @param onLogout Callback for logout action
 */
@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToChatAssistant: () -> Unit,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    // Define bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.BottomBar.Home.route,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = Screen.BottomBar.Wardrobe.route,
            title = "Wardrobe",
            selectedIcon = Icons.Filled.Favorite, // TODO: Replace with wardrobe icon
            unselectedIcon = Icons.Outlined.FavoriteBorder
        ),
        BottomNavItem(
            route = Screen.BottomBar.Looks.route,
            title = "Looks",
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder
        ),
        BottomNavItem(
            route = Screen.BottomBar.AIGeneration.route,
            title = "IA",
            selectedIcon = Icons.Filled.Person, // TODO: Replace with AI icon
            unselectedIcon = Icons.Outlined.Person
        )
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == item.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            // Only navigate if not already on this destination
                            if (!selected) {
                                navController.navigate(item.route) {
                                    // Pop everything up to Home (start destination)
                                    // This prevents stack accumulation when switching tabs
                                    popUpTo(Screen.BottomBar.Home.route) {
                                        saveState = true
                                        inclusive = false // Keep Home in the stack
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.BottomBar.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home tab
            composable(Screen.BottomBar.Home.route) {
                HomeScreen(
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToUpload = onNavigateToUpload,
                    onNavigateToChatAssistant = onNavigateToChatAssistant
                )
            }

            // Wardrobe tab
            composable(Screen.BottomBar.Wardrobe.route) {
                WardrobeListScreen(
                    onNavigateToUpload = onNavigateToUpload
                )
            }

            // Looks tab (stub)
            composable(Screen.BottomBar.Looks.route) {
                LooksScreen()
            }

            // AI Generation tab (stub)
            composable(Screen.BottomBar.AIGeneration.route) {
                AIGenerationScreen()
            }
        }
    }
}
