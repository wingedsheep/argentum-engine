package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shadewing Laureate — Strixhaven: School of Mages #229 (canonical printing)
 * {W}{W/B}{B} · Creature — Human Warlock · 2/2
 *
 * Flying
 * Whenever another creature you control with flying dies, put a +1/+1 counter on target creature
 * you control.
 *
 * The dies trigger is [Triggers.another] over a flying creature you control — the Jackdaw Savior
 * filter shape, matched against the dying creature as it last existed on the battlefield — with
 * a real target chosen as the trigger is put on the stack.
 */
val ShadewingLaureate = card("Shadewing Laureate") {
    manaCost = "{W}{W/B}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever another creature you control with flying dies, put a +1/+1 counter on target " +
        "creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING)).dies()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Whenever another creature you control with flying dies, put a +1/+1 counter " +
            "on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Igor Kieryluk"
        flavorText = "\"Would you prefer I destroy you using prose or verse?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64795a8b-8cf2-436e-8f95-9e8bb40c0d7d.jpg?1783927294"
    }
}
