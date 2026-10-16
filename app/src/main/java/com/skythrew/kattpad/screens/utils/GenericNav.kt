package com.skythrew.kattpad.screens.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.currentBackStackEntryAsState
import com.skythrew.kattpad.R
import com.skythrew.kattpad.screens.HomeScreen
import com.skythrew.kattpad.screens.LibraryScreen
import com.skythrew.kattpad.screens.AccountScreen

@Composable
fun GenericNav(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    NavigationBar {
        NavigationBarItem(selected = route == HomeScreen.javaClass.name,
            onClick = { navController.navigateOnce(HomeScreen) { popUpTo(HomeScreen) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("Discover") })
        NavigationBarItem(selected = route == LibraryScreen.javaClass.name,
            onClick = { navController.navigateOnce(LibraryScreen) { popUpTo(HomeScreen) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(painterResource(R.drawable.outline_menu_book_24), contentDescription = null) }, label = { Text("Library") })
        NavigationBarItem(selected = route == AccountScreen.javaClass.name,
            onClick = { navController.navigateOnce(AccountScreen) { popUpTo(HomeScreen) { saveState = true }; launchSingleTop = true; restoreState = true } },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("Account") })
    }
}

inline fun <reified T: Any> NavController.navigateOnce(route: T, noinline builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentDestination?.route != route.javaClass.name || route is com.skythrew.kattpad.screens.PartScreen || route is com.skythrew.kattpad.screens.ProfileScreen || route is com.skythrew.kattpad.screens.StoryScreen)
        navigate(route, builder = { launchSingleTop = true; builder() })
}
