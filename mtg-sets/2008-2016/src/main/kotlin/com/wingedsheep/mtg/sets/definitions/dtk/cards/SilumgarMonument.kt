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
 * Silumgar Monument
 * {3}
 * Artifact
 *
 * {T}: Add {U} or {B}.
 * {4}{U}{B}: This artifact becomes a 4/4 blue and black Dragon artifact creature with
 * flying until end of turn.
 */
val SilumgarMonument = card("Silumgar Monument") {
    manaCost = "{3}"
    colorIdentity = "UB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U} or {B}.\n" +
        "{4}{U}{B}: This artifact becomes a 4/4 blue and black Dragon artifact creature with flying until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}{B}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Dragon"),
            colors = setOf("BLUE", "BLACK"),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3243e300-1420-4e96-a3d3-610c3723a11f.jpg?1783938567"
    }
}
