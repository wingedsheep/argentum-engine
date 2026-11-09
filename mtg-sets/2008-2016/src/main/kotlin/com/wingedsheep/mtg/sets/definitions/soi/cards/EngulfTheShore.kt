package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Engulf the Shore
 * {3}{U}
 * Instant
 *
 * Return to their owners' hands all creatures with toughness less than or equal to the number
 * of Islands you control.
 *
 * A non-targeted mass bounce ([Patterns.Group.returnAllToHand]) over every creature, any
 * controller, whose projected toughness is at most the number of Islands the caster controls.
 * "Islands" is the land subtype, not basic Islands only — nonbasic lands with the Island type
 * (duals, lands given the type by an effect) count. Both the Island count and each creature's
 * toughness are read at resolution, when the gather runs; every matching creature is returned
 * simultaneously, each to its owner's hand.
 */
val EngulfTheShore = card("Engulf the Shore") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return to their owners' hands all creatures with toughness less than or equal " +
        "to the number of Islands you control."

    spell {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.Creature.compareNumericProperty(
                    CardNumericProperty.TOUGHNESS,
                    ComparisonOperator.LTE,
                    DynamicAmounts.landsWithSubtype(Subtype.ISLAND),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Cliff Childs"
        flavorText = "\"The tides have begun to ignore the moon.\"\n—Tamiyo's journal"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22909767-a088-49ff-83be-37f967d1da3d.jpg?1783937799"
    }
}
