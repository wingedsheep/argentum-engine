package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Warrior's Oath
 * {R}{R}
 * Sorcery
 *
 * Take an extra turn after this one. At the beginning of that turn's end step, you lose the game.
 *
 * The "you lose the game" rider is the `loseAtEndStep` flag on the extra-turn effect (Final
 * Fortune's shape), scoped to the extra turn this spell creates.
 */
val WarriorsOath = card("Warrior's Oath") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."

    spell {
        effect = Effects.TakeExtraTurn(loseAtEndStep = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Mitsuaki Sagiri"
        flavorText = "\"If I fail, my head is yours.\"\n—Warrior's oath common during the Three Kingdoms period"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d582861a-ca6e-4b74-adf0-3eb588ea5ed2.jpg"
    }
}
