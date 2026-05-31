package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bewildering Blizzard
 * {4}{U}{U}
 * Instant
 *
 * Draw three cards. Creatures your opponents control get -3/-0 until end of turn.
 */
val BewilderingBlizzard = card("Bewildering Blizzard") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Creatures your opponents control get -3/-0 until end of turn."

    spell {
        effect = Effects.DrawCards(3).then(
            EffectPatterns.modifyStatsForAll(
                power = -3,
                toughness = 0,
                filter = GroupFilter.AllCreaturesOpponentsControl
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91b25843-1aa0-484a-b6c7-0c284fe7214a.jpg?1743204112"
    }
}
