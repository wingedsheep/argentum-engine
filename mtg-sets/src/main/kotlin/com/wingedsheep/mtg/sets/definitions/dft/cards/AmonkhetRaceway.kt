package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Amonkhet Raceway
 * Land
 *
 * Start your engines!
 * {T}: Add {C}.
 * Max speed — {T}: Target creature gains haste until end of turn.
 */
val AmonkhetRaceway = card("Amonkhet Raceway") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{T}: Add {C}.\n" +
        "Max speed — {T}: Target creature gains haste until end of turn."

    startYourEngines()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Tap
            val creature = target("target creature", Targets.Creature)
            effect = Effects.GrantKeyword(Keyword.HASTE, creature)
            // The auto-rendered label reads "{T}: target gains haste until end of turn" — the bound
            // variable name lands mid-sentence. `maxSpeed { }` prepends "Max speed — " to whichever
            // of the two the ability carries, so the override only needs the cost and the effect.
            description = "{T}: Target creature gains haste until end of turn"
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Brian Valeza"
        flavorText = "Stage 2: The sands and waters of Amonkhet."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f312807-ea2a-4385-8774-4e23b4a5d4a6.jpg?1783907845"
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
