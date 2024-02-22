package com.fsck.k9.net

import okhttp3.OkHttpClient
import org.koin.dsl.module

val networkModule = module {
    single<OkHttpClient> { createOkHttpClient() }
}
