package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Consuming Sinkhole
 * {3}{R}
 * Instant
 * Devoid (This card has no color.)
 * Choose one —
 * • Exile target land creature.
 * • Consuming Sinkhole deals 4 damage to target player or planeswalker.
 *
 * "Land creature" is the conjunction of both types (a permanent that is a land *and* a creature),
 * read off projected state, so a land animated by an effect is a legal target while it is a creature.
 */
val ConsumingSinkhole = card("Consuming Sinkhole") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Choose one —\n" +
        "• Exile target land creature.\n" +
        "• Consuming Sinkhole deals 4 damage to target player or planeswalker."

    keywords(Keyword.DEVOID)

    spell {
        modal(chooseCount = 1) {
            mode("Exile target land creature") {
                val landCreature = target(TargetFilter(GameObjectFilter.Creature and GameObjectFilter.Land))
                effect = Effects.Exile(landCreature)
            }
            mode("Deal 4 damage to target player or planeswalker") {
                val victim = target(Targets.PlayerOrPlaneswalker)
                effect = Effects.DealDamage(4, victim)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Igor Kieryluk"
        flavorText = "\"What will not yield will be consumed.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82a42b28-3d1b-4432-b8c9-2d42e4d0e1c5.jpg?1783937909"

        ruling("2016-01-22", "A “land creature” is a permanent that’s both a land and a creature.")
    }
}
