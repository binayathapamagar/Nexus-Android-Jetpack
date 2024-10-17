package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.dataclass.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val userViewModel: UserViewModel) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    fun searchUsers(query: String) {
        viewModelScope.launch {
            val allUsers = userViewModel.users.value
            _searchResults.value = allUsers.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.fullName.contains(query, ignoreCase = true)
            }
        }
    }

    // Factory class to instantiate SearchViewModel with UserViewModel
    class Factory(private val userViewModel: UserViewModel) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(userViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
