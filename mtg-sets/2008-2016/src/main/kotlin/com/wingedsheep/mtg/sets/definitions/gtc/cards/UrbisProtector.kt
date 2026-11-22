package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Urbis Protector
 * {4}{W}{W}
 * Creature — Human Cleric
 * 1/1
 * When this creature enters, create a 4/4 white Angel creature token with flying.
 *
 * Gatecrash printed its own 4/4 white Angel token, so the art comes from GTC's token registry.
 */
val UrbisProtector = card("Urbis Protector") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, create a 4/4 white Angel creature token with flying."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Steve Argyle"
        flavorText = "\"I hear that Boros is losing its angels to the Gateless Movement. I'm sure this comes as no surprise to you.\"\n—Teysa, to Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acf932ac-5ea5-491b-b555-5e9ea971d93d.jpg"
    }
}
