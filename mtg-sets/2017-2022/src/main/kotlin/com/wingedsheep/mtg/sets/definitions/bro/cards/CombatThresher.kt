package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Combat Thresher
 * {7}
 * Artifact Creature — Construct
 * 3/3
 * Prototype {2}{W} — 1/1
 * Double strike
 * When this creature enters, draw a card.
 */
val CombatThresher = card("Combat Thresher") {
    manaCost = "{7}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 3
    oracleText = "Prototype {2}{W} — 1/1 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Double strike\n" +
        "When this creature enters, draw a card."

    keywordAbility(KeywordAbility.prototype("{2}{W}", 1, 1))

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74dfabfd-e13b-4512-a733-abe514be6404.jpg?1783920117"
    }
}
