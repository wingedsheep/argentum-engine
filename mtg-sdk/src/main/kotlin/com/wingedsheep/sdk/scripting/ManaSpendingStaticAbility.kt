package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [players] may spend [fromColor] mana as though it were [toColor] mana, for any mana payment
 * (CR 609.4b). The permission changes neither the cost nor the actual color or restrictions of the
 * mana: a `{C}` pip still needs colorless mana, and "spend this mana only on X" riders still apply.
 *
 * - [fromColor] `null` is "mana" of every type — each color **and colorless** (Mycosynth Lattice's
 *   ruling: "players [may] spend even colorless mana as though it had a color").
 * - [toColor] `null` is "mana of any color" — every colored requirement accepts it.
 * - [players] is who holds the permission, read relative to this permanent's controller:
 *   [Player.You] (Sunglasses of Urza, the default), [Player.Each] ("Players may spend …",
 *   Mycosynth Lattice) or [Player.EachOpponent].
 */
@SerialName("SpendManaAsColor")
@Serializable
data class SpendManaAsColor(
    val fromColor: Color?,
    val toColor: Color?,
    val players: Player = Player.You
) : StaticAbility {
    init {
        require(players == Player.You || players == Player.Each || players == Player.EachOpponent) {
            "SpendManaAsColor.players must be You, Each or EachOpponent, was $players"
        }
    }

    override val description: String = buildString {
        append(
            when (players) {
                Player.Each -> "Players may"
                Player.EachOpponent -> "Your opponents may"
                else -> "You may"
            }
        )
        append(" spend ")
        append(fromColor?.let { "${it.name.lowercase()} mana" } ?: "mana")
        append(" as though it were ")
        append(toColor?.let { "${it.name.lowercase()} mana" } ?: "mana of any color")
        append(".")
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFrom = fromColor?.let(replacer::replaceColor)
        val newTo = toColor?.let(replacer::replaceColor)
        return if (newFrom == fromColor && newTo == toColor) this else copy(fromColor = newFrom, toColor = newTo)
    }
}
