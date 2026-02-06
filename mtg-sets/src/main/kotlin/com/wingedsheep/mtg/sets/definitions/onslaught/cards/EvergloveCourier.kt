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
 * Everglove Courier
 * {2}{G}
 * Creature — Elf
 * 2/1
 * {T}: Target Elf creature gets +2/+2 and gains trample until end of turn.
 */
val EvergloveCourier = card("Everglove Courier") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0)) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.TRAMPLE, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Wayne Reynolds"
        flavorText = "Speed and power—an elf needs nothing else."
        imageUri = "https://cards.scryfall.io/large/front/1/3/13bf5786-e41a-4839-b8a0-5c7a413b23d0.jpg?1562899727"
    }
}
