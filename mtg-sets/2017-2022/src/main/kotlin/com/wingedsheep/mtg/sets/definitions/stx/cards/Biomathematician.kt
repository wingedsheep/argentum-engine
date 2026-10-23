package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Biomathematician — Strixhaven: School of Mages #164 (canonical printing)
 * {1}{G}{U} · Creature — Human Wizard · 2/2
 *
 * When this creature enters, create a 0/0 green and blue Fractal creature token. Put a +1/+1
 * counter on each Fractal you control.
 *
 * The Fractal is created, then [Effects.ForEachInGroup] over every Fractal permanent you control
 * (the new token and any earlier ones) adds one +1/+1 counter each — the Oracle's Gift shape.
 * State-based actions aren't checked mid-resolution, so the 0/0 token survives to get its counter.
 */
val Biomathematician = card("Biomathematician") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, create a 0/0 green and blue Fractal creature token. " +
        "Put a +1/+1 counter on each Fractal you control."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.FRACTAL)).youControl(),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Igor Kieryluk"
        flavorText = "\"I will reach infinity.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b41a35ef-dae1-4534-a12a-d3951b285273.jpg?1783927324"
    }
}
