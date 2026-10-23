package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Lens of Clarity
 * {1}
 * Artifact
 * You may look at the top card of your library and at face-down creatures you don't control any time.
 *
 * Ruling (2014-09-20): Lens of Clarity lets you look at the top card of your library and at
 * face-down creatures you don't control whenever you want, even if you don't have priority.
 * Ruling (2014-09-20): Lens of Clarity doesn't let you look at face-down spells you don't
 * control on the stack.
 */
val LensOfClarity = card("Lens of Clarity") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may look at the top card of your library and at face-down creatures you don't control any time."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = LookAtFaceDownCreatures
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Raymond Swanland"
        flavorText = "\"Water shifts and confuses, but as ice it holds the stillness of truth.\"\n—Arel the Whisperer"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c82e2b8-f0f6-44da-83ee-8a671344ac62.jpg?1562785174"
        ruling("2014-09-20", "Lens of Clarity lets you look at the top card of your library and at face-down creatures you don't control whenever you want, even if you don't have priority.")
        ruling("2014-09-20", "Lens of Clarity doesn't stop your opponents from looking at face-down creatures they control.")
        ruling("2014-09-20", "Lens of Clarity doesn't let you look at face-down spells you don't control on the stack.")
    }
}
