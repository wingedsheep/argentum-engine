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
 * Ojutai Monument
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {U}.
 * {4}{W}{U}: This artifact becomes a 4/4 white and blue Dragon artifact creature with
 * flying until end of turn.
 */
val OjutaiMonument = card("Ojutai Monument") {
    manaCost = "{3}"
    colorIdentity = "WU"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {U}.\n" +
        "{4}{W}{U}: This artifact becomes a 4/4 white and blue Dragon artifact creature with flying until end of turn."

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
        cost = Costs.Mana("{4}{W}{U}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Dragon"),
            colors = setOf("WHITE", "BLUE"),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ef29942-a7e3-4e47-9f95-527a5d7e4f1d.jpg?1783938567"
    }
}
