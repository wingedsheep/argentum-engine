package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gretchen Titchwillow
 * {G}{U}
 * Legendary Creature — Halfling Druid
 * 0/4
 * {2}{G}{U}: Draw a card. You may put a land card from your hand onto the battlefield.
 *
 * The land half is Zimone, Quandrix Prodigy's [Patterns.Hand.putFromHand] (its up-to-one
 * selection is the "you may"), untapped here — not a land play, so it ignores land drops.
 */
val GretchenTitchwillow = card("Gretchen Titchwillow") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Halfling Druid"
    power = 0
    toughness = 4
    oracleText = "{2}{G}{U}: Draw a card. You may put a land card from your hand onto the battlefield."

    activatedAbility {
        cost = Costs.Mana("{2}{G}{U}")
        effect = Effects.DrawCards(1) then Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Land,
            prompt = "You may put a land card from your hand onto the battlefield"
        )
        description = "{2}{G}{U}: Draw a card. You may put a land card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Mila Pesic"
        flavorText = "\"Plant what you wish to see grow. Listen for the stories you want to hear told.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c8b248e-1460-4650-9721-85c134c21b89.jpg?1783926448"
        ruling("2021-07-23", "Putting a land onto the battlefield this way doesn't count as playing a land and can be done even if you've played a land this turn or if it isn't your turn.")
    }
}
