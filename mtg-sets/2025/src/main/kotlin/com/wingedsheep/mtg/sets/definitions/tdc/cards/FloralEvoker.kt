package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Floral Evoker
 * {2}{G}
 * Creature — Snake Druid
 * 2/3
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 * {G}, Discard a creature card: Return target land card from your graveyard to the battlefield tapped.
 */
val FloralEvoker = card("Floral Evoker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Druid"
    power = 2
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.\n" +
        "{G}, Discard a creature card: Return target land card from your graveyard to the battlefield tapped."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Discard(GameObjectFilter.Creature))
        val t = target(TargetFilter(GameObjectFilter.Land.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefield(t, tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Monztre"
        flavorText = "\"Death is a natural thing. It replenishes the land's energy and nourishes the growth of all life.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4452f54b-1b44-4ccf-be99-7d3c2614d89a.jpg?1783907165"
    }
}
