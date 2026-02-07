package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Frightshroud Courier
 * {2}{B}
 * Creature — Zombie
 * 2/1
 * You may choose not to untap Frightshroud Courier during your untap step.
 * {2}{B}, {T}: Target Zombie creature gets +2/+2 and gains fear for as long as Frightshroud Courier remains tapped.
 */
val FrightshroudCourier = card("Frightshroud Courier") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1

    keywords(Keyword.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Zombie"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.FEAR, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/large/front/4/a/4a0fa75a-a82b-44cd-965f-07e0fe7a111a.jpg?1562912314"
    }
}
