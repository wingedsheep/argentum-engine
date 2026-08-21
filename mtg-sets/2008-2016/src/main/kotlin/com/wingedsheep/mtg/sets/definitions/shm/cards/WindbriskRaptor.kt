package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Windbrisk Raptor
 * {5}{W}{W}
 * Creature — Bird
 * 5 / 7
 *
 * Flying
 * Attacking creatures you control have lifelink.
 *
 * - "Attacking" is a *state* predicate on the group filter, not a duration: creatures gain lifelink
 *   the moment they're declared as attackers and lose it when they stop attacking, which is the
 *   same shape as Crossway Troublemakers.
 * - No `excludeSelf` — the Raptor itself has lifelink while it is attacking.
 */
val WindbriskRaptor = card("Windbrisk Raptor") {
    manaCost = "{5}{W}{W}"
    typeLine = "Creature — Bird"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "Attacking creatures you control have lifelink."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(
            Keyword.LIFELINK,
            GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Omar Rayyan"
        flavorText = "It awakened to gloom-fouled skies and responded with a righteous rage that shook the heavens."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5df7b710-d44c-4ebc-be5b-f81d697086c4.jpg?1783942764"
    }
}
