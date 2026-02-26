package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.AbilityFlag
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
    oracleText = "You may choose not to untap Frightshroud Courier during your untap step.\n{2}{B}, {T}: Target Zombie creature gets +2/+2 and gains fear for as long as Frightshroud Courier remains tapped."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Zombie"))
        ))
        effect = ModifyStatsEffect(2, 2, t, Duration.WhileSourceTapped()) then
                GrantKeywordUntilEndOfTurnEffect(Keyword.FEAR, t, Duration.WhileSourceTapped())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a0fa75a-a82b-44cd-965f-07e0fe7a111a.jpg?1562912314"
    }
}
