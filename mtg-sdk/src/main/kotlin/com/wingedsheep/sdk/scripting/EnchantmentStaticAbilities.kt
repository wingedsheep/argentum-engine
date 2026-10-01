package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Prevents matching Auras from enchanting the affected permanents. */
@SerialName("PreventEnchantment")
@Serializable
data class PreventEnchantment(
    val auras: GameObjectFilter = GameObjectFilter.Enchantment.withSubtype("Aura"),
    val exceptSource: Boolean = false,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} can't be enchanted by ${if (exceptSource) "other " else ""}matching Auras"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAuras = auras.applyTextReplacement(replacer)
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newAuras !== auras || newFilter !== filter) copy(auras = newAuras, filter = newFilter) else this
    }
}
