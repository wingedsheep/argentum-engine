package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Pearlspear Courier
 * {2}{W}
 * Creature — Human Soldier
 * 2/1
 * You may choose not to untap Pearlspear Courier during your untap step.
 * {2}{W}, {T}: Target Soldier creature gets +2/+2 and has vigilance for as long as Pearlspear Courier remains tapped.
 */
val PearlspearCourier = card("Pearlspear Courier") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "You may choose not to untap Pearlspear Courier during your untap step.\n{2}{W}, {T}: Target Soldier creature gets +2/+2 and has vigilance for as long as Pearlspear Courier remains tapped."

    keywords(Keyword.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.VIGILANCE, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/large/front/a/1/a1ea7219-6ab6-471a-afe7-d7da1df434c7.jpg?1562933222"
    }
}
