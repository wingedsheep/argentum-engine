package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Entrails Feaster
 * {B}
 * Creature — Zombie Cat
 * 1/1
 * At the beginning of your upkeep, you may exile a creature card from a graveyard.
 * If you do, put a +1/+1 counter on Entrails Feaster.
 * If you don't, tap Entrails Feaster.
 */
val EntrailsFeaster = card("Entrails Feaster") {
    manaCost = "{B}"
    typeLine = "Creature — Zombie Cat"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, you may exile a creature card from a graveyard. If you do, put a +1/+1 counter on Entrails Feaster. If you don't, tap Entrails Feaster."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        val t = target("target", TargetObject(
            filter = TargetFilter.CreatureInGraveyard
        ))
        effect = CompositeEffect(
            listOf(
                MoveToZoneEffect(t, Zone.EXILE),
                AddCountersEffect("+1/+1", 1, EffectTarget.Self)
            )
        )
        elseEffect = TapUntapEffect(EffectTarget.Self, tap = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdddab92-3e1f-49dc-afd0-8c84d0d952c2.jpg?1562943619"
    }
}
