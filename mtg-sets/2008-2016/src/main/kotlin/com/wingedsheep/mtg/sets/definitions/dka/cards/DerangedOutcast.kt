package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deranged Outcast
 * {1}{G}
 * Creature — Human Rogue
 * 2/1
 * {1}{G}, Sacrifice a Human: Put two +1/+1 counters on target creature.
 */
val DerangedOutcast = card("Deranged Outcast") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "{1}{G}, Sacrifice a Human: Put two +1/+1 counters on target creature."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN))
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Nils Hamm"
        flavorText = "Cast out by his village, he now takes grim offense at refugees who seek shelter in his forest."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b35fee-8e24-4d89-ad77-d55d06bb1d7f.jpg"
    }
}
