package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Slick Imitator
 * {1}{U}
 * Creature — Ooze
 * 1/3
 * Start your engines!
 * Max speed — {1}, Sacrifice this creature: Copy target spell you control. You may choose new
 * targets for the copy.
 *
 * The copy is unrestricted by spell type, so it can duplicate a permanent spell — the copy
 * resolves into a token, which [Effects.CopyTargetSpell] already handles.
 */
val SlickImitator = card("Slick Imitator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Ooze"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — {1}, Sacrifice this creature: Copy target spell you control. You may choose " +
        "new targets for the copy. (A copy of a permanent spell becomes a token.)"
    power = 1
    toughness = 3

    startYourEngines()

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
            val spell = target("target spell you control", Targets.SpellYouControl)
            effect = Effects.CopyTargetSpell(spell)
            // `maxSpeed { }` prepends "Max speed — "; the auto-rendered label would otherwise
            // read the bound variable name mid-sentence.
            description = "{1}, Sacrifice this creature: Copy target spell you control"
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e86ef50-4939-4e7c-853d-438f0f3e0411.jpg?1783907902"
        ruling("2025-02-07", "A player \"has max speed\" if their speed is 4.")
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
