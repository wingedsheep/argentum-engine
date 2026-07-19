package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Frantic Firebolt
 * {2}{R}
 * Instant
 * Frantic Firebolt deals X damage to target creature, where X is 2 plus the number of cards in your
 * graveyard that are instant cards, sorcery cards, and/or have an Adventure.
 *
 * X is counted at resolution (CR 608.2) from the current graveyard contents. A card is counted once
 * even if it satisfies more than one clause (instant *and* has an Adventure), because the tally is a
 * single [Filters.InstantSorceryOrAdventure] membership test rather than three separate counts.
 */
val FranticFirebolt = card("Frantic Firebolt") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Frantic Firebolt deals X damage to target creature, where X is 2 plus the number " +
        "of cards in your graveyard that are instant cards, sorcery cards, and/or have an Adventure."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(
            DynamicAmount.Add(
                DynamicAmount.Fixed(2),
                DynamicAmount.Count(Player.You, Zone.GRAVEYARD, Filters.InstantSorceryOrAdventure)
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Olivier Bernard"
        flavorText = "Johann's conjecture about draconic fire dispelling rainwater elementals proved " +
            "to be correct. Now he just needed a way to dispel draconic fire."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efd85f5a-258b-4ced-bf9e-3abe7fe72395.jpg?1783915094"
    }
}
