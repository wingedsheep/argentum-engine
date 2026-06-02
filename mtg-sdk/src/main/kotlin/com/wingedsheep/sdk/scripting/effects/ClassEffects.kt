package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Level up a Class enchantment to the specified target level.
 * Used as the effect for the sorcery-speed level-up activated ability
 * that the engine generates for Class enchantments.
 *
 * @param targetLevel The level to advance to (e.g., 2 or 3)
 */
@SerialName("LevelUpClass")
@Serializable
data class LevelUpClassEffect(
    val targetLevel: Int
) : Effect {
    override val description: String = "Level up to level $targetLevel"
}
