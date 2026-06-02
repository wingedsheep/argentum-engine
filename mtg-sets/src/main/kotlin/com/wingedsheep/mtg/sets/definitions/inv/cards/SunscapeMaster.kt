package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sunscape Master
 * {2}{W}{W}
 * Creature — Human Wizard
 * 2/2
 * {G}{G}, {T}: Creatures you control get +2/+2 until end of turn.
 * {U}{U}, {T}: Return target creature to its owner's hand.
 */
val SunscapeMaster = card("Sunscape Master") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{G}{G}, {T}: Creatures you control get +2/+2 until end of turn.\n" +
        "{U}{U}, {T}: Return target creature to its owner's hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            ModifyStatsEffect(2, 2, EffectTarget.Self)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{U}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Alan Rabinowitz"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebb7203d-529d-45d2-8e03-cd342c153f38.jpg?1562942364"
    }
}
