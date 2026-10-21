package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trelasarra, Moon Dancer
 * {G}{W}
 * Legendary Creature — Elf Cleric
 * 2/2
 * Whenever you gain life, put a +1/+1 counter on Trelasarra and scry 1.
 */
val TrelasarraMoonDancer = card("Trelasarra, Moon Dancer") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Cleric"
    oracleText = "Whenever you gain life, put a +1/+1 counter on Trelasarra and scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then Effects.Scry(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Kieran Yanner"
        flavorText = "\"Eilistraee teaches us that we are not bound by the circumstances of our birth. We all may find beauty and light, if we have the courage to seek them.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6a4b54c-a8fa-464e-a3dd-f3f3a08606f5.jpg?1783926442"
    }
}
