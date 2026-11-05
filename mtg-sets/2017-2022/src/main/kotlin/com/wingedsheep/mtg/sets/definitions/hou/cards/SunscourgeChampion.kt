package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunscourge Champion — Hour of Devastation #26 (canonical printing)
 * {2}{W} · Creature — Human Wizard · 2/3
 *
 * When this creature enters, you gain life equal to its power.
 * Eternalize—{2}{W}{W}, Discard a card.
 *
 * The eternalized 4/4 token copies the enters trigger, so it gains 4.
 */
val SunscourgeChampion = card("Sunscourge Champion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you gain life equal to its power.\n" +
        "Eternalize—{2}{W}{W}, Discard a card. ({2}{W}{W}, Discard a card, Exile this card from your graveyard: " +
        "Create a token that's a copy of it, except it's a 4/4 black Zombie Human Wizard with no mana cost. " +
        "Eternalize only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
    }

    eternalize("{2}{W}{W}", Costs.DiscardCard)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0bb41cc-6edb-4a31-92cb-b17850b49432.jpg?1783936056"
        ruling("2017-07-14", "The amount of life you gain is determined as Sunscourge Champion's triggered ability resolves. Players may respond to the ability by attempting to change its power.")
        ruling("2017-07-14", "If Sunscourge Champion leaves the battlefield before its triggered ability resolves, use its power as it last existed on the battlefield to determine how much life to gain. If that number is negative, you don't gain any life and you don't lose any life.")
        ruling("2017-07-14", "Several cards have an eternalize cost that includes \"Discard a card.\" You can't discard the card with eternalize to pay its own cost because the card has to be in your graveyard to begin activating its eternalize ability.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
    }
}
