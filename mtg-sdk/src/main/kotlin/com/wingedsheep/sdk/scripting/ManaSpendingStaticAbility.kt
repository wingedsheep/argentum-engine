package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A controller may spend [fromColor] mana as though it were [toColor], for any mana payment.
 * The permission changes neither the cost nor the actual color or restrictions of the mana.
 */
@SerialName("SpendManaAsColor")
@Serializable
data class SpendManaAsColor(val fromColor: Color, val toColor: Color) : StaticAbility {
    override val description: String =
        "You may spend ${fromColor.name.lowercase()} mana as though it were ${toColor.name.lowercase()} mana."

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFrom = replacer.replaceColor(fromColor)
        val newTo = replacer.replaceColor(toColor)
        return if (newFrom == fromColor && newTo == toColor) this else copy(fromColor = newFrom, toColor = newTo)
    }
}
