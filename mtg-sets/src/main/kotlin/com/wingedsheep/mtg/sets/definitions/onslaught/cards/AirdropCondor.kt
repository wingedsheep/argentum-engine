package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Airdrop Condor
 * {4}{R}
 * Creature — Bird
 * 2/2
 * Flying
 * {1}{R}, Sacrifice a Goblin creature: Airdrop Condor deals damage equal to
 * the sacrificed creature's power to any target.
 */
val AirdropCondor = card("Airdrop Condor") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\n{1}{R}, Sacrifice a Goblin creature: Airdrop Condor deals damage equal to the sacrificed creature's power to any target."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(
            amount = DynamicAmount.SacrificedPermanentPower,
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Glen Angus"
        flavorText = "It has two kinds of droppings, neither of which is particularly pleasant."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9796ac-11e2-4295-bf00-f684d0111970.jpg?1562951282"
    }
}
