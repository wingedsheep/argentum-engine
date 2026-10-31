package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Clockwork Servant
 * {3}
 * Artifact Creature — Gnome
 * 2/3
 * Adamant — When this creature enters, if at least three mana of the same color was spent to cast
 * it, draw a card.
 *
 * "The same color" is any one of the five colors, so the intervening-if is the OR of three-or-more
 * of each color spent on the (all-generic) cost. Not cast, or cast without paying mana: no draw.
 */
private val threeOfOneColorSpent = Conditions.Any(
    *Color.entries.map { color ->
        Conditions.CompareAmounts(DynamicAmounts.manaOfColorSpent(color), ComparisonOperator.GTE, 3)
    }.toTypedArray()
)

val ClockworkServant = card("Clockwork Servant") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Gnome"
    power = 2
    toughness = 3
    oracleText = "Adamant — When this creature enters, if at least three mana of the same color was spent to cast it, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = threeOfOneColorSpent
        effect = Effects.DrawCards(1)
        description = "Adamant — When this creature enters, if at least three mana of the same color was spent to cast it, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "David Palumbo"
        flavorText = "Some are annoyed by the constant ticking as they clean the cobbles, but to the castle custodians, there's no sweeter sound."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3af5942-d171-401a-9444-3b59c579e4db.jpg?1783932589"
        ruling("2019-10-04", "Adamant effects check what mana was actually spent to cast a spell. If an effect allows you to spend mana \"as though it were mana\" of any color or type, that allows you to spend mana you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell.")
        ruling("2019-10-04", "If you copy a spell that has an adamant ability, no mana was spent to cast the copy at all, so that ability won't apply.")
    }
}
