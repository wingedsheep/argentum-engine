package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Levitating Statue
 * {2}
 * Artifact
 * Flying
 * Whenever you cast a noncreature spell, put a +1/+1 counter on this artifact.
 * {2}: This artifact becomes a 1/1 Construct artifact creature until end of turn.
 */
val LevitatingStatue = card("Levitating Statue") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Flying\n" +
        "Whenever you cast a noncreature spell, put a +1/+1 counter on this artifact.\n" +
        "{2}: This artifact becomes a 1/1 Construct artifact creature until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Construct"),
            addTypes = setOf("ARTIFACT"),
            duration = Duration.EndOfTurn,
        )
        description = "{2}: This artifact becomes a 1/1 Construct artifact creature until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c0a17e2-e019-4686-9795-651da2d2955c.jpg?1783920017"
    }
}
