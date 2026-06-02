package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Thunderscape Master
 * {2}{R}{R}
 * Creature — Human Wizard
 * 2/2
 * {B}{B}, {T}: Target player loses 2 life and you gain 2 life.
 * {G}{G}, {T}: Creatures you control get +2/+2 until end of turn.
 */
val ThunderscapeMaster = card("Thunderscape Master") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "RBG"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{B}{B}, {T}: Target player loses 2 life and you gain 2 life.\n" +
        "{G}{G}, {T}: Creatures you control get +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{B}"), Costs.Tap)
        val t = target("target player", TargetPlayer())
        effect = Effects.LoseLife(2, t) then Effects.GainLife(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.Tap)
        description = "{G}{G}, {T}: Creatures you control get +2/+2 until end of turn."
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            effect = ModifyStatsEffect(2, 2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22abdc2f-bdc8-46c4-8ce2-f06befedbc32.jpg?1562901918"
    }
}
