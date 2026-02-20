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
 * Everglove Courier
 * {2}{G}
 * Creature — Elf
 * 2/1
 * You may choose not to untap Everglove Courier during your untap step.
 * {2}{G}, {T}: Target Elf creature gets +2/+2 and gains trample for as long as Everglove Courier remains tapped.
 */
val EvergloveCourier = card("Everglove Courier") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 1
    oracleText = "You may choose not to untap Everglove Courier during your untap step.\n{2}{G}, {T}: Target Elf creature gets +2/+2 and gains trample for as long as Everglove Courier remains tapped."

    keywords(Keyword.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
        )
        effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.TRAMPLE, EffectTarget.ContextTarget(0), Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "262"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/large/front/1/3/13bf5786-e41a-4839-b8a0-5c7a413b23d0.jpg?1562899727"
    }
}
