package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Failed Conversion
 * {4}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -4/-4.
 * When enchanted creature dies, surveil 2.
 *
 * The dies trigger belongs to the Aura (its controller surveils), not the enchanted creature.
 */
val FailedConversion = card("Failed Conversion") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -4/-4.\n" +
        "When enchanted creature dies, surveil 2. (Look at the top two cards of your library, " +
        "then put any number of them into your graveyard and the rest on top of your library " +
        "in any order.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-4, -4)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/358c18b7-5321-4793-b54a-e48e39548b9c.jpg?1783917011"
        ruling(
            "2024-01-12",
            "When you surveil, you may put all the cards you look at back on top of your library, " +
                "you may put all of those cards into your graveyard, or you may put some of those " +
                "cards on top and the rest of them into your graveyard."
        )
    }
}
