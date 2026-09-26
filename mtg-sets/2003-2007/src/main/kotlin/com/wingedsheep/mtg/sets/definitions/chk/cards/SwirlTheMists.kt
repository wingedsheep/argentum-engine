package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChangeAllColorWordsToChosenColor
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * Swirl the Mists — Champions of Kamigawa #94 (canonical printing)
 * {2}{U}{U} · Enchantment
 *
 * As this enchantment enters, choose a color word.
 * All instances of color words in the text of spells and permanents are changed to the chosen
 * color word.
 *
 * A global Layer 3 text change: while it's on the battlefield, "protection from red",
 * "target nonblack creature" and every other color word on a spell or permanent reads as the
 * chosen color. Mana symbols and the objects' own colors are untouched.
 */
val SwirlTheMists = card("Swirl the Mists") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a color word.\n" +
        "All instances of color words in the text of spells and permanents are changed to the chosen color word."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    staticAbility {
        ability = ChangeAllColorWordsToChosenColor
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ea50e09-4ab3-439e-83d7-d584f8af8f16.jpg?1783944319"
        ruling(
            "2004-12-01",
            "This effect lasts as long as Swirl the Mists is on the battlefield. It ends as soon as " +
                "Swirl the Mists leaves the battlefield."
        )
        ruling(
            "2004-12-01",
            "Swirl the Mists' ability also affects spells that are cast and permanents that enter " +
                "after Swirl the Mists enters."
        )
    }
}
