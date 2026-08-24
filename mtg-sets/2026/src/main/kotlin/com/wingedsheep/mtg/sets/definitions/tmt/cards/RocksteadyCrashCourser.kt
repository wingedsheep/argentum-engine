package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rocksteady, Crash Courser
 * {4}{G}{G}
 * Legendary Creature — Rhino Mutant
 * 7/7
 *
 * Rocksteady can't be blocked by more than one creature.
 * Boars you control can't be blocked by more than one creature.
 * Forestcycling {2}
 */
val RocksteadyCrashCourser = card("Rocksteady, Crash Courser") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Rhino Mutant"
    oracleText = "Rocksteady can't be blocked by more than one creature.\nBoars you control can't be blocked by more than one creature.\nForestcycling {2} ({2}, Discard this card: Search your library for a Forest card, reveal it, put it into your hand, then shuffle.)"
    power = 7
    toughness = 7

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(
            maxBlockers = 1,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Boar").youControl())
        )
    }

    keywordAbility(KeywordAbility.typecycling("Forest", "{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Filipe Pagliuso"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebbb5756-80fa-406f-8daa-be1f5a6c3c80.jpg?1771586968"
    }
}
