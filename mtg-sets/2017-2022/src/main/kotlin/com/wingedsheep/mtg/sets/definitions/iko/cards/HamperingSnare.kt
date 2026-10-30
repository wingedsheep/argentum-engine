package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hampering Snare — Ikoria: Lair of Behemoths #55
 * {1}{U} · Instant
 *
 * Creatures your opponents control get -2/-0 until end of turn.
 * Cycling {2}
 */
val HamperingSnare = card("Hampering Snare") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -2/-0 until end of turn.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -2,
            toughness = 0,
            filter = GroupFilter.AllCreaturesOpponentsControl
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Svetlin Velinov"
        flavorText = "\"I want it to look me in the eyes and know the face of the man who put it down.\"\n—Kelsien, the Plague"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f7b331a-bd21-429b-a49f-88da9a31c98e.jpg"
    }
}
