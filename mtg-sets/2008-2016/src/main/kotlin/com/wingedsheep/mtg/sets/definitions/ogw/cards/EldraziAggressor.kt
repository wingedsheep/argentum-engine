package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Eldrazi Aggressor
 * {2}{R}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * This creature has haste as long as you control another colorless creature.
 */
val EldraziAggressor = card("Eldrazi Aggressor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "This creature has haste as long as you control another colorless creature."

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE.name, GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Mathias Kollros"
        flavorText = "\"They employ a foul sort of teamwork.\"\n—General Tazri, allied commander"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62f3ec85-552d-4e28-939e-ab2c39e3e9c5.jpg?1783937909"
        ruling(
            "2016-01-22",
            "If it's the turn Eldrazi Aggressor comes under your control, and it loses haste after being declared as an attacker, it will continue to attack. It won't be removed from combat."
        )
    }
}
