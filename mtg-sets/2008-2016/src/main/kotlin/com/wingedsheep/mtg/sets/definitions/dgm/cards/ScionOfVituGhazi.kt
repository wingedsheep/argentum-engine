package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Scion of Vitu-Ghazi
 * {3}{W}{W}
 * Creature — Elemental
 * 4/4
 * When this creature enters, if you cast it from your hand, create a 1/1 white Bird creature token
 * with flying, then populate. (Create a token that's a copy of a creature token you control.)
 *
 * The intervening-if is [Conditions.WasCastFromHand] (as on Reiver Demon). Populate chooses — never
 * targets — a creature token you control on resolution, so the Bird just created is always a legal
 * choice: [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens, as on Muster the Departed.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val ScionOfVituGhazi = card("Scion of Vitu-Ghazi") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, if you cast it from your hand, create a 1/1 white Bird " +
        "creature token with flying, then populate. (Create a token that's a copy of a creature token " +
        "you control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCastFromHand
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/b/4b0c59f8-b407-47e7-8885-8c968f4ceecf.jpg?1783914993"
        ) then Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cd20865-0a9a-4a72-92f9-77c8d6384b46.jpg?1783940044"
    }
}
