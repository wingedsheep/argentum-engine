package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Flamestick Courier
 * {2}{R}
 * Creature — Goblin
 * 2/1
 * {T}: Target Goblin creature gets +2/+2 and gains haste until end of turn.
 */
val FlamestickCourier = card("Flamestick Courier") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0)) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.HASTE, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Carl Critchlow"
        flavorText = "\"We've got a special delivery of pain for ya.\""
        imageUri = "https://cards.scryfall.io/large/front/e/8/e822161d-0434-4578-aecd-c9ef0b84bd4e.jpg?1562950280"
    }
}
