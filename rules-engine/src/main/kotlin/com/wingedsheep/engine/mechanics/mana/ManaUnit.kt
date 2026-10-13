package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

/** A source-tagged unrestricted unit; its color remains the color actually produced. */
@Serializable
data class ManaUnit(val color: Color?, val source: ManaSourceTag)

/** Match the same plain/snow buckets that the payment deducted, with oldest tagged units first. */
internal fun List<ManaUnit>.consume(color: Color?, plain: Int, snow: Int): List<ManaUnit> {
    if (plain <= 0 && snow <= 0) return this
    var plainLeft = plain
    var snowLeft = snow
    return filter { unit ->
        if (unit.color != color) true
        else if (unit.source.isSnow && snowLeft > 0) { snowLeft--; false }
        else if (!unit.source.isSnow && plainLeft > 0) { plainLeft--; false }
        else true
    }
}
