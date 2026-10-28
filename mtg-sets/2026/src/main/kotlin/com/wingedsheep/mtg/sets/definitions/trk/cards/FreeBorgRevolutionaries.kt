package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Free Borg Revolutionaries
 * {3}{W}{W}
 * Artifact Creature — Borg Rebel
 * 4/5
 *
 * When this creature enters, creatures you control get +1/+1 until end of turn.
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 */
val FreeBorgRevolutionaries = card("Free Borg Revolutionaries") {
    manaCost = "{3}{W}{W}"
    typeLine = "Artifact Creature — Borg Rebel"
    colorIdentity = "W"
    oracleText = "When this creature enters, creatures you control get +1/+1 until end of turn.\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 4
    toughness = 5
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Smirtouille"
        flavorText = "\"Resistance is *not* futile.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6711e910-e5a8-4602-9559-de5374bffff2.jpg?1785981345"
    }
}

