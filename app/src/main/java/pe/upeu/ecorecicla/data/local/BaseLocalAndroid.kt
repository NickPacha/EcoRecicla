package pe.upeu.ecorecicla.data.local

import android.content.Context
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class BaseLocalAndroid(context: Context) {
    val database: EcoDatabase by lazy {
        val driver = AndroidSqliteDriver(EcoDatabase.Schema, context, "ecorecicla.db")
        EcoDatabase(
            driver = driver,
            PuntoEntityAdapter = PuntoEntity.Adapter(
                secuenciaAdapter = IntColumnAdapter
            )
        )
    }
}
