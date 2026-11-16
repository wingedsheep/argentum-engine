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
 * Kolaghan Monument
 * {3}
 * Artifact
 *
 * {T}: Add {B} or {R}.
 * {4}{B}{R}: This artifact becomes a 4/4 black and red Dragon artifact creature with
 * flying until end of turn.
 */
val KolaghanMonument = card("Kolaghan Monument") {
    manaCost = "{3}"
    colorIdentity = "BR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B} or {R}.\n" +
        "{4}{B}{R}: This artifact becomes a 4/4 black and red Dragon artifact creature with flying until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}{R}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Dragon"),
            colors = setOf("BLACK", "RED"),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/620cddb7-abf7-4367-8440-815b886db8e0.jpg?1783938568"
    }
}
