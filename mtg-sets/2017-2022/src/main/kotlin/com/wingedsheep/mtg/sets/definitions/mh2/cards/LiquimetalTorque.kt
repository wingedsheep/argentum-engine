package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Liquimetal Torque
 * {2}
 * Artifact
 * {T}: Add {C}.
 * {T}: Target nonland permanent becomes an artifact in addition to its other types until end of turn.
 *
 * "In addition to its other types" is the additive [Effects.AddCardType] (Stone by Sunlight's
 * shape) with [Duration.EndOfTurn] — the permanent keeps every other type, subtype and ability.
 */
val LiquimetalTorque = card("Liquimetal Torque") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Target nonland permanent becomes an artifact in addition to its other types until end of turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.AddCardType("ARTIFACT", permanent, Duration.EndOfTurn)
        description = "{T}: Target nonland permanent becomes an artifact in addition to its other types until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Brian Snõddy"
        flavorText = "\"Weaponized jewelry has a long and respected history.\"\n—Ervos Trax"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13c6101a-da40-4785-8ccb-4e779bbbdb55.jpg?1783926804"
    }
}
