package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cold Case Cracker
 * {3}{U}
 * Creature — Spirit Detective
 * 3/3
 * Flying
 * When this creature dies, investigate.
 */
val ColdCaseCracker = card("Cold Case Cracker") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit Detective"
    oracleText = "Flying\n" +
        "When this creature dies, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Wayne Wu"
        flavorText = "\"It's a rare privilege to investigate one's own death.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f082111b-9b1c-4a25-8c5d-d6ef77533a9b.jpg?1783912913"
    }
}
