package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blood Servitor
 * {3}
 * Artifact Creature — Construct
 * 2/2
 *
 * When this creature enters, create a Blood token.
 */
val BloodServitor = card("Blood Servitor") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a Blood token. (It's an artifact with \"{1}, {T}, " +
        "Discard a card, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateBlood(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Jason A. Engle"
        flavorText = "\"Please, my friends, help yourselves to the help.\"\n—Olivia Voldaren"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87845cc2-bf5d-491d-bfa2-b33b034557a4.jpg?1782703017"
    }
}
