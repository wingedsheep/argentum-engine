package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ravenous Squirrel
 * {B/G}
 * Creature — Squirrel
 * 1/1
 *
 * Whenever you sacrifice an artifact or creature, put a +1/+1 counter on this creature.
 * {1}{B}{G}, Sacrifice an artifact or creature: You gain 1 life and draw a card.
 */
val RavenousSquirrel = card("Ravenous Squirrel") {
    manaCost = "{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Squirrel"
    oracleText = "Whenever you sacrifice an artifact or creature, put a +1/+1 counter on this creature.\n" +
        "{1}{B}{G}, Sacrifice an artifact or creature: You gain 1 life and draw a card."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{G}"),
            Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature)
        )
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Dan Murayama Scott"
        flavorText = "\"Nope, no monsters in the forest. Nothing in there but squirrels.\"\n—Joskun, An-Havva constable"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f44a15c-1bb4-4fb8-88a0-e4d3f2dee1b4.jpg"
    }
}
