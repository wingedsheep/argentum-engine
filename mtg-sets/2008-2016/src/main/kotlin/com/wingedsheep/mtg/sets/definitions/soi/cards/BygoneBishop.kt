package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bygone Bishop (Shadows over Innistrad #8)
 * {2}{W}
 * Creature — Spirit Cleric 2/3
 * Flying
 * Whenever you cast a creature spell with mana value 3 or less, investigate.
 *
 * The filter reads the spell's mana value, not the cost paid — casting a creature for an
 * alternative cost (madness) doesn't change its mana value (see ruling).
 */
val BygoneBishop = card("Bygone Bishop") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you cast a creature spell with mana value 3 or less, investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.manaValueAtMost(3))
        effect = Effects.Investigate()
        description = "Whenever you cast a creature spell with mana value 3 or less, investigate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c45198f-fb2b-4c83-abdc-8b1a0071cfed.jpg?1783937823"
        ruling(
            "2016-04-08",
            "Casting a spell for an alternative cost, such as a madness cost, doesn't change its mana value. " +
                "For example, casting Twins of Maurer Estate (a card with mana cost {4}{B} and madness cost {2}{B}) " +
                "for its madness cost will not cause Bygone Bishop's ability to trigger."
        )
    }
}
