package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Pollywog Symbiote
 * {1}{U}
 * Creature — Frog
 * 1/3
 * Each creature spell you cast costs {1} less to cast if it has mutate.
 * Whenever you cast a creature spell, if it has mutate, draw a card, then discard a card.
 *
 * "Has mutate" reads the spell's printed keywords, so the reduction applies whether the spell is
 * cast for its mana cost or its mutate cost (cost reductions apply to the total cost, alternative
 * cost included). The intervening "if it has mutate" can't change between trigger and resolution
 * (the spell's keywords are fixed on the stack, and last-known information keeps them if it left),
 * so it is expressed as the cast trigger's spell filter.
 */
val PollywogSymbiote = card("Pollywog Symbiote") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog"
    power = 1
    toughness = 3
    oracleText = "Each creature spell you cast costs {1} less to cast if it has mutate.\n" +
        "Whenever you cast a creature spell, if it has mutate, draw a card, then discard a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.withKeyword(Keyword.MUTATE)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withKeyword(Keyword.MUTATE))
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Simon Dominic"
        flavorText = "It always knew it was destined to be more than a frog."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77adf375-e7d7-4ebd-8c86-ad7e822a5483.jpg?1783931070"
    }
}
