package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/** Fang-Druid Summoner — Aetherdrift #163. */
val FangDruidSummoner = card("Fang-Druid Summoner") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ape Druid"
    power = 2
    toughness = 4
    oracleText = "Reach\n" +
        "When this creature enters, you may search your library and/or graveyard for a creature " +
        "card with no abilities, reveal it, and put it into your hand. If you search your library " +
        "this way, shuffle."

    keywords(Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
            filter = Filters.CreatureWithNoAbilities,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/496442f6-48c7-464e-bcf3-4c14f49fa065.jpg?1783907871"
    }
}
