package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Helm of the Gods
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 for each enchantment you control.
 * Equip {1}
 *
 * The bonus is a [GrantDynamicStats] that recounts the enchantments you control continuously,
 * so it grows and shrinks as enchantments enter and leave.
 */
val HelmOfTheGods = card("Helm of the Gods") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each enchantment you control.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count()
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Anthony Palumbo"
        flavorText = "Its blessings are as numerous as the stars of Nyx."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1ba006c8-0ffc-4e68-9a55-c7b842d3add9.jpg?1783938309"
    }
}
