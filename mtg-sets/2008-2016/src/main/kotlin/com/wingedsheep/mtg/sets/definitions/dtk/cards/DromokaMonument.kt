package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dromoka Monument
 * {3}
 * Artifact
 *
 * {T}: Add {G} or {W}.
 * {4}{G}{W}: This artifact becomes a 4/4 green and white Dragon artifact creature with
 * flying until end of turn.
 */
val DromokaMonument = card("Dromoka Monument") {
    manaCost = "{3}"
    colorIdentity = "GW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G} or {W}.\n" +
        "{4}{G}{W}: This artifact becomes a 4/4 green and white Dragon artifact creature with flying until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}{W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Dragon"),
            colors = setOf("GREEN", "WHITE"),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4df975f-b6ef-4e8e-8be8-529c0ffd8a37.jpg?1783938569"
    }
}
