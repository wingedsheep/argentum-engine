package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Lu Xun, Scholar General
 * {2}{U}{U}
 * Legendary Creature — Human Soldier
 * 1/3
 * Horsemanship
 * Whenever Lu Xun deals damage to an opponent, you may draw a card.
 */
val LuXunScholarGeneral = card("Lu Xun, Scholar General") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Soldier"
    power = 1
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\nWhenever Lu Xun deals damage to an opponent, you may draw a card."

    keywordAbility(KeywordAbility.Simple(Keyword.HORSEMANSHIP))

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Xu Xiaoming"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0882d4c8-32f1-4d86-b5c5-75a16697004c.jpg?1783946121"
    }
}
