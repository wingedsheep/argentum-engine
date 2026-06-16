package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Malcolm, the Eyes
 * {U}{R}
 * Legendary Creature — Siren Pirate
 * 2/2
 *
 * Flying, haste
 * Whenever you cast your second spell each turn, investigate. (Create a Clue token. It's an
 * artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * "Cast your second spell each turn" is [Triggers.NthSpellCast]; "investigate" is the keyword
 * action [Effects.Investigate] (create a Clue token).
 */
val MalcolmTheEyes = card("Malcolm, the Eyes") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Siren Pirate"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\n" +
        "Whenever you cast your second spell each turn, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        effect = Effects.Investigate()
        description = "Whenever you cast your second spell each turn, investigate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Dmitry Burmak"
        flavorText = "\"It's funny how rarely folks here think to look up.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/521dffaa-813b-41e4-b7c2-a8c407167875.jpg?1712356159"
    }
}
