package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Wayward Swordtooth
 * {2}{G}
 * Creature — Dinosaur
 * 5/5
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * You may play an additional land on each of your turns.
 * This creature can't attack or block unless you have the city's blessing.
 *
 * Ascend on a permanent is the keyword alone — the engine's state-based check grants the city's
 * blessing whenever you control ten permanents (CR 702.131b), as on Tendershoot Dryad. The attack
 * and block restrictions are two static `CantAttackUnless` / `CantBlockUnless` keyed to
 * `Conditions.YouHaveCitysBlessing`, the Lupine Prototype shape.
 */
val WaywardSwordtooth = card("Wayward Swordtooth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "You may play an additional land on each of your turns.\n" +
        "This creature can't attack or block unless you have the city's blessing."
    power = 5
    toughness = 5

    keywords(Keyword.ASCEND)

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }
    staticAbility {
        ability = CantAttackUnless(Conditions.YouHaveCitysBlessing)
    }
    staticAbility {
        ability = CantBlockUnless(Conditions.YouHaveCitysBlessing)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/351e8b1b-4e4e-4ffc-a134-3cf0e2a1dd6d.jpg?1783935278"
        ruling(
            "2018-01-19",
            "Wayward Swordtooth's middle ability is cumulative if you control more than one. It's also " +
                "cumulative with other effects that let you play additional lands, such as the one from Enter the Unknown."
        )
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose " +
                "control of some or all of your permanents."
        )
    }
}
