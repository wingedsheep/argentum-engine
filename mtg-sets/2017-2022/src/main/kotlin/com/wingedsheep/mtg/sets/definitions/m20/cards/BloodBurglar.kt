package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Blood Burglar — Core Set 2020 #88
 * {1}{B} · Creature — Vampire Rogue · Common
 * 2/2
 *
 * During your turn, this creature has lifelink.
 *
 * A self-scoped [GrantKeyword] gated by [Conditions.IsYourTurn]; projection re-evaluates it as
 * the active player changes, so lifelink is present only on its controller's turn.
 */
val BloodBurglar = card("Blood Burglar") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 2
    oracleText = "During your turn, this creature has lifelink. " +
        "(Damage dealt by this creature also causes you to gain that much life.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Cristi Balanescu"
        flavorText = "\"You've heard that a vampire can't enter a home without an invitation? " +
            "A lie meant to help you sleep at night.\"\n—Kerin, watch sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01efd5af-ed6d-4132-8f33-37f6a9fa55d0.jpg?1783932998"
    }
}
