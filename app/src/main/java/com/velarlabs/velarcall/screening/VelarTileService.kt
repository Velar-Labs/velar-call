package com.velarlabs.velarcall.screening

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.velarlabs.velarcall.data.Settings

/** Atalho nas Configurações Rápidas do Android para ligar/desligar a proteção. */
class VelarTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val settings = Settings(this)
        settings.protectionOn = !settings.protectionOn
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val on = Settings(this).protectionOn
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) "Protegido" else "Desligado"
        tile.updateTile()
    }
}
