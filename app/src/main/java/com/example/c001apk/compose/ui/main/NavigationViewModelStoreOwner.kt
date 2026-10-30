package com.example.c001apk.compose.ui.main

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal class NavigationViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    parent: HasDefaultViewModelProviderFactory,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val defaultViewModelProviderFactory = parent.defaultViewModelProviderFactory
    override val defaultViewModelCreationExtras = parent.defaultViewModelCreationExtras
}
