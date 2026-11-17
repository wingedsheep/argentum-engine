package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bloodfire Enforcers
 * {3}{R}
 * Creature — Human Monk
 * 5/2
 * This creature has first strike and trample as long as an instant card and a sorcery card are
 * in your graveyard.
 */
val BloodfireEnforcers = card("Bloodfire Enforcers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Monk"
    power = 5
    toughness = 2
    oracleText = "This creature has first strike and trample as long as an instant card and a " +
        "sorcery card are in your graveyard."

    val instantAndSorceryInGraveyard = Conditions.All(
        Conditions.GraveyardContains(GameObjectFilter.Instant),
        Conditions.GraveyardContains(GameObjectFilter.Sorcery)
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source()),
            condition = instantAndSorceryInGraveyard
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = instantAndSorceryInGraveyard
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Yefim Kligerman"
        flavorText = "Bloodfire practitioners train to focus their rage, building it deep inside " +
            "themselves until they are ready to release it as a devastating attack."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e72f0a37-32d6-4c1f-858d-7eec096dd42a.jpg?1783938690"
    }
}
