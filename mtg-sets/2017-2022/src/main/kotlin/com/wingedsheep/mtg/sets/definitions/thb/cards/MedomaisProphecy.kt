package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Medomai's Prophecy
 * {1}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Scry 2.
 * II — Choose a card name.
 * III — When you cast a spell with the chosen name for the first time this turn, draw two cards.
 * IV — Look at the top card of each player's library.
 *
 * Chapter II records the name durably on the Saga (`recordOnSource`), because chapter III reads it
 * in a later turn's resolution. Chapter III's delayed trigger is the ordinal cast trigger over spells
 * with that name — `castsNth(1, namedFromChosenComponent())` — with the name frozen into the trigger
 * as it's created, and it lasts only this turn. With no name chosen it can never trigger (ruling).
 * Chapter IV is a lone non-destructive gather of each library's top card (Mishra's Bauble's look).
 */
val MedomaisProphecy = card("Medomai's Prophecy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Scry 2.\n" +
        "II — Choose a card name.\n" +
        "III — When you cast a spell with the chosen name for the first time this turn, draw two cards.\n" +
        "IV — Look at the top card of each player's library."

    sagaChapter(1) {
        effect = Patterns.Library.scry(2)
    }

    sagaChapter(2) {
        effect = Effects.ChooseCardName(recordOnSource = true)
    }

    sagaChapter(3) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.castsNth(1, GameObjectFilter.Any.namedFromChosenComponent()),
            effect = Effects.DrawCards(2),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    sagaChapter(4) {
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1, Player.Each))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2bd7b4-28de-4d9e-86c5-a46bd608cb02.jpg?1783931583"
    }
}
