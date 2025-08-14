package com.weatherapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.weatherapp.model.MainViewModel
import com.weatherapp.ui.pages.HomePage
import com.weatherapp.ui.pages.ListPage
import com.weatherapp.ui.pages.MapPage

@Composable
fun MainNavHost(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController, startDestination = Route.Home) {
        composable<Route.Home> { HomePage(Modifier,viewModel) }
        composable<Route.List> { ListPage(Modifier,viewModel) }
        composable<Route.Map> { MapPage(viewModel) }
    }
}