package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Infernal Reckoning
 * {B}
 * Instant
 * Exile target colorless creature. You gain life equal to its power.
 *
 * The life gain is sequenced before the exile, the same house pattern as Swords to Plowshares:
 * target references read the live board, so gaining first reads the creature's projected power
 * while it is still on the battlefield — the last-known value the ruling asks for. A negative
 * power gains nothing (the life-gain primitive ignores amounts of 0 or less).
 */
val InfernalReckoning = card("Infernal Reckoning") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target colorless creature. You gain life equal to its power."

    spell {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless))
        )
        effect = Effects.GainLife(DynamicAmounts.powerOf(creature)) then
            Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Bram Sels"
        flavorText = "\"When these intruders are gone, Zendikar will be my throne, my palace, my playground.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e291f27-d74f-48e7-bcb4-ddcc558c2211.jpg?1783934570"
        ruling("2018-07-13", "The amount of life gained is equal to the power of the creature as it last existed on the battlefield.")
        ruling("2018-07-13", "If the creature's power is negative, you don't lose or gain life.")
    }
}
