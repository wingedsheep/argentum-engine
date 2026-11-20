package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mnemonic Wall
 * {4}{U}
 * Creature — Wall
 * 0/4
 * Defender
 * When this creature enters, you may return target instant or sorcery card from your graveyard to
 * your hand.
 */
val MnemonicWall = card("Mnemonic Wall") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender\n" +
        "When this creature enters, you may return target instant or sorcery card from your graveyard to your hand."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.May(Effects.ReturnToHand(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Vance Kovacs"
        flavorText = "\"I'd build an entire fortress of them if I could.\"\n—Mzali, Lighthouse archmage"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffaebb4a-cb9b-4ac8-b661-3e63d9d984f4.jpg?1783941994"
    }
}
