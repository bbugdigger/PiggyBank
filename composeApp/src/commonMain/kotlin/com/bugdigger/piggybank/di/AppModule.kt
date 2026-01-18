package com.bugdigger.piggybank.di

import com.bugdigger.piggybank.data.api.PiggyBankApi
import com.bugdigger.piggybank.ui.viewmodel.AuthViewModel
import com.bugdigger.piggybank.ui.viewmodel.AccountsViewModel
import com.bugdigger.piggybank.ui.viewmodel.RegisterViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for dependency injection
 */
val appModule = module {
    // API client - singleton
    single { PiggyBankApi() }
    
    // ViewModels
    viewModelOf(::AuthViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::RegisterViewModel)
}
