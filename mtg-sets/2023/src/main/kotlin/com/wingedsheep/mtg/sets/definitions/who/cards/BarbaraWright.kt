package com.wingedsheep.mtg.sets.definitions.who.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Barbara Wright
 * {1}{W}
 * Legendary Creature — Human Advisor
 * 1/3
 * History Teacher — Sagas you control have read ahead. (As a Saga enters, choose a chapter and
 * start with that many lore counters. Skipped chapters don't trigger.)
 * Doctor's companion (You can have two commanders if the other is the Doctor.)
 *
 *  - **History Teacher** — `GrantKeyword(READ_AHEAD, Sagas you control)`. The engine synthesizes
 *    the read-ahead chapter choice for a Saga entering under the grant (granted-read-ahead
 *    synthesis at the entry seams), and the chapter-skip rule reads the projected keyword.
 *  - **Doctor's companion** — a deck-construction partner ability (CR 702.124m).
 */
val BarbaraWright = card("Barbara Wright") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 3
    oracleText = "History Teacher — Sagas you control have read ahead. (As a Saga enters, choose a " +
        "chapter and start with that many lore counters. Skipped chapters don't trigger.)\n" +
        "Doctor's companion (You can have two commanders if the other is the Doctor.)"

    staticAbility {
        ability = GrantKeyword(
            Keyword.READ_AHEAD,
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SAGA).youControl()),
        )
    }

    keywordAbility(KeywordAbility.doctorsCompanion())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Irina Nordsol"
        flavorText = "\"There will be no sacrifice this afternoon, Doctor. Or ever again.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/414bdd33-549d-4690-917c-ea48decffbbb.jpg?1783914681"
    }
}
