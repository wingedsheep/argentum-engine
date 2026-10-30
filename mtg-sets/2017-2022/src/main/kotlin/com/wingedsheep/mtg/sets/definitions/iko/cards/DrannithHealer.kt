package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Drannith Healer
 * {1}{W}
 * Creature — Human Cleric
 * 2/2
 * Whenever you cycle another card, you gain 1 life.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * "Another" needs no filter (see Drannith Stinger): the trigger only functions on the
 * battlefield, and a card can only be cycled from hand.
 */
val DrannithHealer = card("Drannith Healer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever you cycle another card, you gain 1 life.\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.GainLife(1)
        description = "Whenever you cycle another card, you gain 1 life."
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Scott Murphy"
        flavorText = "\"The glowstones shine and dinosaurs roar to the south. Business will be brisk tonight.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5a821c-eaec-4f69-97c7-8299cdebc2f4.jpg?1783931092"
    }
}
