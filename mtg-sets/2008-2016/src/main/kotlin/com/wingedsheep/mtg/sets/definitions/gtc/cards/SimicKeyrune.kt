package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Simic Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {G} or {U}.
 * {G}{U}: This artifact becomes a 2/3 green and blue Crab artifact creature with hexproof
 * until end of turn.
 *
 * The animation is additive: `BecomeCreature` keeps the Artifact type and `addTypes` restates it,
 * matching the printed "artifact creature". The default target is the Keyrune itself and the
 * default duration is end of turn.
 */
val SimicKeyrune = card("Simic Keyrune") {
    manaCost = "{3}"
    colorIdentity = "GU"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G} or {U}.\n" +
        "{G}{U}: This artifact becomes a 2/3 green and blue Crab artifact creature with hexproof until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
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
        cost = Costs.Mana("{G}{U}")
        effect = Effects.BecomeCreature(
            power = 2,
            toughness = 3,
            keywords = setOf(Keyword.HEXPROOF),
            creatureTypes = setOf("Crab"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.GREEN.name, Color.BLUE.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d039aae6-38f5-42b5-a530-e0dd03abc7d5.jpg?1783940091"
    }
}
