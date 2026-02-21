package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Flamestick Courier
 * {2}{R}
 * Creature — Goblin
 * 2/1
 * You may choose not to untap Flamestick Courier during your untap step.
 * {2}{R}, {T}: Target Goblin creature gets +2/+2 and gains haste for as long as Flamestick Courier remains tapped.
 */
val FlamestickCourier = card("Flamestick Courier") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "You may choose not to untap Flamestick Courier during your untap step.\n{2}{R}, {T}: Target Goblin creature gets +2/+2 and gains haste for as long as Flamestick Courier remains tapped."

    keywords(Keyword.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
        ))
        effect = ModifyStatsEffect(2, 2, t, Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.HASTE, t, Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/large/front/e/8/e822161d-0434-4578-aecd-c9ef0b84bd4e.jpg?1562950280"
    }
}
