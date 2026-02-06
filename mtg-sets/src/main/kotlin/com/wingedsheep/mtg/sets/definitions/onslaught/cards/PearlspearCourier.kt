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
 * Pearlspear Courier
 * {2}{W}
 * Creature — Human Soldier
 * 2/1
 * {T}: Target Soldier creature gets +2/+2 and gains vigilance until end of turn.
 */
val PearlspearCourier = card("Pearlspear Courier") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0)) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.VIGILANCE, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Mark Zug"
        flavorText = "Theirs is an order of action, not talk."
        imageUri = "https://cards.scryfall.io/large/front/a/1/a1ea7219-6ab6-471a-afe7-d7da1df434c7.jpg?1562933222"
    }
}
