package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gempalm Sorcerer
 * {2}{U}
 * Creature — Human Wizard Sorcerer
 * 2/2
 * Cycling {2}{U}
 * When you cycle Gempalm Sorcerer, Wizard creatures gain flying until end of turn.
 */
val GempalmSorcerer = card("Gempalm Sorcerer") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard Sorcerer"
    oracleText = "Cycling {2}{U}\nWhen you cycle Gempalm Sorcerer, Wizard creatures gain flying until end of turn."
    power = 2
    toughness = 2

    keywordAbility(KeywordAbility.cycling("{2}{U}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.GrantKeywordToAll(Keyword.FLYING, GroupFilter.allCreaturesWithSubtype("Wizard"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67bda65b-2e26-4531-9f6a-952df314c8f7.jpg?1767959965"
    }
}
