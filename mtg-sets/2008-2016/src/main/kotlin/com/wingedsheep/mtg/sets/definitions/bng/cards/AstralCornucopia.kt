package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters

/**
 * Astral Cornucopia
 * {X}{X}{X}
 * Artifact
 * This artifact enters with X charge counters on it.
 * {T}: Choose a color. Add one mana of that color for each charge counter on this artifact.
 *
 * The entry is Orochi Hatchery's X-charge-counter replacement over the cast's X. The mana ability
 * is "N mana of any one color" with N read from the charge counters as it resolves.
 */
val AstralCornucopia = card("Astral Cornucopia") {
    manaCost = "{X}{X}{X}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters with X charge counters on it.\n" +
        "{T}: Choose a color. Add one mana of that color for each charge counter on this artifact."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.CHARGE,
            count = DynamicAmounts.castX()
        )
    )

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddAnyColorMana(DynamicAmounts.countersOnSelf(CounterType.CHARGE))
        description = "{T}: Choose a color. Add one mana of that color for each charge counter on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a72b8011-c712-418f-869e-42fda3dc0830.jpg?1783939522"
        ruling(
            "2014-02-01",
            "If you choose 1 for the value of X, Astral Cornucopia will cost {3} to cast and enter the " +
                "battlefield with one charge counter. If you choose 2 for the value of X, it will cost {6} to " +
                "cast and enter the battlefield with two charge counters, and so on."
        )
    }
}
