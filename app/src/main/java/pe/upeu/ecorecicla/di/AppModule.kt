package pe.upeu.ecorecicla.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.AlmacenSeguroAndroid
import pe.upeu.ecorecicla.data.local.BaseLocalAndroid
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.repository.AuthRepositoryImpl
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.data.repository.RutaRepositoryImpl
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaDiaViewModel
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel

val appModule = module {
    single { AlmacenSeguroAndroid(get()) }
    single { BaseLocalAndroid(get()).database }
    singleOf(::ClienteHttp)

    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::RutaRepositoryImpl) bind RutaRepository::class
    singleOf(::PesajeRepositoryImpl) bind PesajeRepository::class

    singleOf(::SincronizarUseCase)

    viewModelOf(::LoginViewModel)
    viewModelOf(::RutaDiaViewModel)
    viewModelOf(::DetallePuntoViewModel)
    viewModelOf(::PerfilViewModel)
}
