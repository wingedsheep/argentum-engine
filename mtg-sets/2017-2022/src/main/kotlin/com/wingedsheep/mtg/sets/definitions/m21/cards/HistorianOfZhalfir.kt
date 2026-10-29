package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Historian of Zhalfir — Core Set 2021 #325 (canonical printing)
 * {2}{U}{U} · Creature — Human Wizard · 3/3
 *
 * Whenever this creature attacks, if you control a Teferi planeswalker, draw a card.
 *
 * "If you control a Teferi planeswalker" is an intervening-if (CR 603.4) on the planeswalker
 * subtype, the Adherent of Hope shape.
 */
val HistorianOfZhalfir = card("Historian of Zhalfir") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, if you control a Teferi planeswalker, draw a card."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Teferi"))
        effect = Effects.DrawCards(1)
        description = "Whenever this creature attacks, if you control a Teferi planeswalker, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "325"
        artist = "Denman Rooke"
        flavorText = "\"Come and listen. I'll tell you the story of a nation lost to time, and of the man who caused it.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae981da0-f32c-49d5-bcb0-2b9255a4e1fe.jpg?1783930621"
    }
}
