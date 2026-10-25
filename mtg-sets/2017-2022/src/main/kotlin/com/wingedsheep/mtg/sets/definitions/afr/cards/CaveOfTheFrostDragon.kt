package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cave of the Frost Dragon
 * Land
 * If you control two or more other lands, this land enters tapped.
 * {T}: Add {W}.
 * {4}{W}: This land becomes a 3/4 white Dragon creature with flying until end of turn.
 * It's still a land.
 */
val CaveOfTheFrostDragon = card("Cave of the Frost Dragon") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "If you control two or more other lands, this land enters tapped.\n{T}: Add {W}.\n" +
        "{4}{W}: This land becomes a 3/4 white Dragon creature with flying until end of turn. It's still a land."

    replacementEffect(
        EntersTapped(unlessCondition = Conditions.Not(Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Dragon"),
            colors = setOf("WHITE"),
            duration = Duration.EndOfTurn,
        )
        description = "{4}{W}: This land becomes a 3/4 white Dragon creature with flying until end of turn. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb41396-5008-4a5f-92ca-54ecac42e926.jpg?1783926436"
    }
}
