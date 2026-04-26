package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Foraging Wickermaw
 * {2}
 * Artifact Creature — Scarecrow
 * 1/3
 *
 * When this creature enters, surveil 1.
 * {1}: Add one mana of any color. This creature becomes that color until end of turn.
 *      Activate only once each turn.
 */
val ForagingWickermaw = card("Foraging Wickermaw") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)\n" +
        "{1}: Add one mana of any color. This creature becomes that color until end of turn. " +
        "Activate only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        effect = Effects.Composite(
            Effects.AddAnyColorMana(1),
            Effects.BecomeChosenManaColor()
        )
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{1}: Add one mana of any color. This creature becomes that color until end of turn. " +
            "Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "256"
        artist = "Ron Spencer"
        flavorText = "It feeds on beauty others have left behind."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f524bc08-caeb-4362-b960-eb8e0e4159d0.jpg?1767957290"
    }
}
