package com.weatherapp.model

import ForecastMonitor
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.weatherapp.api.WeatherService
import com.weatherapp.api.toForecast
import com.weatherapp.api.toWeather
import com.weatherapp.repo.Repository
import com.weatherapp.ui.nav.Route

class MainViewModel(
    private val repository: Repository,
    private val service: WeatherService,
    internal val monitor: ForecastMonitor
) : ViewModel(), Repository.Listener {

    private var _page = mutableStateOf<Route>(Route.Home)
    var page: Route
        get() = _page.value
        set(tmp) {
            _page.value = tmp
        }

    private var _city = mutableStateOf<City?>(null)
    var city: City?
        get() = _city.value
        set(tmp) {
            _city.value = tmp?.copy()
        }

    private val _cities = mutableStateMapOf<String, City>()
    val cities: List<City> get() = _cities.values.toList()

    private val _user = mutableStateOf<User?>(null)
    val user: User? get() = _user.value

    init {
        repository.setListener(this)
    }

    fun remove(city: City) {
        repository.remove(city)
    }

    fun add(name: String, location: LatLng? = null) {
        val newCity = City(name = name, location = location)
        repository.add(newCity)
    }

    fun add(name: String) {
        service.getLocation(name) { lat, lng ->
            if (lat != null && lng != null) {
                repository.add(City(name = name, location = LatLng(lat, lng), isMonitored = true))
            }
        }
    }

    fun add(location: LatLng) {
        service.getName(location.latitude, location.longitude) { name ->
            if (name != null) {
                repository.add(City(name = name, location = location, isMonitored = true))
            }
        }
    }

    override fun onUserLoaded(user: User) {
        _user.value = user
    }

    override fun onUserSignOut() {
        monitor.cancelAll()
    }

    override fun onCityAdded(city: City) {
        _cities[city.name!!] = city
        monitor.updateCity(city)
    }

    override fun onCityUpdated(city: City) {
        val oldCity = _cities[city.name]
        _cities[city.name!!] = city.copy(
            weather = oldCity?.weather,
            forecast = oldCity?.forecast
        )
        if (_city.value?.name == city.name) {
            _city.value = _cities[city.name]
        }
        monitor.updateCity(city)
    }

    override fun onCityRemoved(city: City) {
        _cities.remove(city.name)
        if (_city.value?.name == city.name) _city.value = null
        monitor.cancelCity(city)
    }

    fun loadWeather(name: String) {
        service.getWeather(name) { apiWeather ->
            val newCity = _cities[name]?.copy(weather = apiWeather?.toWeather())
            if (newCity != null) _cities[name] = newCity
        }
    }

    fun loadForecast(name: String) {
        service.getForecast(name) { apiForecast ->
            val newCity = _cities[name]?.copy(forecast = apiForecast?.toForecast())
            if (newCity != null) {
                _cities[name] = newCity
                if (city?.name == name) city = newCity
            }
        }
    }

    fun loadBitmap(name: String) {
        val city = _cities[name] ?: return
        service.getBitmap(city.weather?.imgUrl ?: return) { bitmap ->
            val newCity = city.copy(
                weather = city.weather.copy(bitmap = bitmap)
            )
            _cities[name] = newCity
        }
    }

    fun update(city: City) {
        _city.value = city
        _cities[city.name] = city
        repository.update(city)
    }

    class MainViewModelFactory(
        private val repository: Repository,
        private val service: WeatherService,
        private val monitor: ForecastMonitor
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, service, monitor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
