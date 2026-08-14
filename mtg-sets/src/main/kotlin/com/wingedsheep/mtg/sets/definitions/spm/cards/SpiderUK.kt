package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-UK — Marvel's Spider-Man #17
 * {3}{W} · Legendary Creature — Spider Human Hero · 3/4
 *
 * Web-slinging {2}{W}
 * At the beginning of your end step, if two or more creatures entered the battlefield under your
 * control this turn, you draw a card and gain 2 life.
 *
 * The end-step clause is an intervening-'if' trigger (CR 603.4) gated on
 * [Conditions.CreaturesEnteredThisTurn]`(atLeast = 2)` — the creature-typed entry counter.
 */
val SpiderUK = card("Spider-UK") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 3
    toughness = 4
    oracleText = "Web-slinging {2}{W} (You may cast this spell for {2}{W} if you also return a " +
        "tapped creature you control to its owner's hand.)\n" +
        "At the beginning of your end step, if two or more creatures entered the battlefield under " +
        "your control this turn, you draw a card and gain 2 life."

    webSlinging("{2}{W}")

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CreaturesEnteredThisTurn(atLeast = 2)
        effect = Effects.DrawCards(1) then Effects.GainLife(2)
        description = "At the beginning of your end step, if two or more creatures entered the " +
            "battlefield under your control this turn, you draw a card and gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Allen Morris"
        flavorText = "\"Spiders across the Multiverse are dying. I must keep them safe.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6beb4548-1fab-4b9e-bf24-f7b9aadecc87.jpg?1783905359"
    }
}
