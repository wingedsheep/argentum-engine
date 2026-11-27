package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bladed Bracers
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * As long as equipped creature is a Human or an Angel, it has vigilance.
 * Equip {2}
 */
val BladedBracers = card("Bladed Bracers") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "As long as equipped creature is a Human or an Angel, it has vigilance.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility { ability = ModifyStats(1, 1, Filters.EquippedCreature) }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withAnySubtype("Human", "Angel"),
        )
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Ryan Yee"
        flavorText = "Forged from the rubble of the Helvault."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/897a5116-043c-46aa-880e-be8dcb1618bc.jpg?1783940655"
    }
}
