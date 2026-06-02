package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Rabbit Response
 * {2}{W}{W}
 * Instant
 * Creatures you control get +2/+1 until end of turn. If you control a Rabbit, scry 2.
 */
val RabbitResponse = card("Rabbit Response") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+1 until end of turn. If you control a Rabbit, scry 2."

    spell {
        effect = GroupPatterns.modifyStatsForAll(
            2, 1,
            GroupFilter(GameObjectFilter.Creature.youControl())
        ).then(ConditionalEffect(
            condition = Conditions.ControlCreatureOfType(Subtype("Rabbit")),
            effect = LibraryPatterns.scry(2)
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Rovina Cai"
        flavorText = "You never fight just one rabbitfolk. They bring the strength of their burrow to battle with them."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4ded450-346d-4917-917a-b62bc0267509.jpg?1721425917"
    }
}
