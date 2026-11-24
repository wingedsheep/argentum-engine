package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Azorius Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {U}.
 * {W}{U}: This artifact becomes a 2/2 white and blue Bird artifact creature with flying
 * until end of turn.
 *
 * Same shape as the Gatecrash Keyrunes: the animation is additive, `addTypes` restates the
 * printed "artifact creature", and the default target/duration is the Keyrune itself until
 * end of turn.
 */
val AzoriusKeyrune = card("Azorius Keyrune") {
    manaCost = "{3}"
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {U}.\n" +
        "{W}{U}: This artifact becomes a 2/2 white and blue Bird artifact creature with flying until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Bird"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.WHITE.name, Color.BLUE.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Daniel Ljunggren"
        flavorText = "\"The higher the mind soars, the greater its understanding of the law.\"\n—Isperia"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23a05db7-dcae-4180-8ad1-60ba6fb30816.jpg?1783940325"
    }
}
