package com.example.c001apk.compose.ui.main

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class NavigationViewModelStoreOwnerTest {
    private val dependencyKey = object : CreationExtras.Key<String> {}

    private val parent = object : HasDefaultViewModelProviderFactory {
        override val defaultViewModelCreationExtras = MutableCreationExtras().apply {
            this[dependencyKey] = "injected dependency"
        }
        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return modelClass.cast(DependentViewModel(checkNotNull(extras[dependencyKey])))!!
            }
        }
    }

    @Test
    fun createsViewModelWithTheParentFactoryAndExtrasInsideTheSessionStore() {
        val owner = NavigationViewModelStoreOwner(ViewModelStore(), parent)
        val model = ViewModelProvider.create(owner)[DependentViewModel::class.java]
        assertEquals("injected dependency", model.dependency)
        assertSame(model, ViewModelProvider.create(owner)[DependentViewModel::class.java])
        owner.viewModelStore.clear()
    }

    @Test
    fun separateAccountStoresDoNotShareViewModels() {
        val first = NavigationViewModelStoreOwner(ViewModelStore(), parent)
        val second = NavigationViewModelStoreOwner(ViewModelStore(), parent)
        assertNotSame(
            ViewModelProvider.create(first)[DependentViewModel::class.java],
            ViewModelProvider.create(second)[DependentViewModel::class.java],
        )
        first.viewModelStore.clear()
        second.viewModelStore.clear()
    }

    private class DependentViewModel(val dependency: String) : ViewModel()
}
