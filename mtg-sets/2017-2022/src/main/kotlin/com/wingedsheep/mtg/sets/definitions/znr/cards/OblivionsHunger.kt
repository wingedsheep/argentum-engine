package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oblivion's Hunger
 * {1}{B}
 * Instant
 * Target creature you control gains indestructible until end of turn. Draw a card if that
 * creature has a +1/+1 counter on it.
 */
val OblivionsHunger = card("Oblivion's Hunger") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature you control gains indestructible until end of turn. Draw a card if that creature has a +1/+1 counter on it. (Damage and effects that say \"destroy\" don't destroy the creature.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature) then
            Effects.If(
                condition = Conditions.TargetHasCounter(CounterType.PLUS_ONE_PLUS_ONE, creature),
                then = Effects.DrawCards(1)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Izzy"
        flavorText = "\"Indulge the void. Become the void. Consume all you touch.\"\n—Drana, the last bloodchief"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/faf65512-8228-48f4-ba7b-d861b66d28c9.jpg?1783929368"
        ruling("2020-09-25", "If the target creature is an illegal target by the time Oblivion's Hunger tries to resolve, the spell doesn't resolve. You don't draw a card if the target has or had a +1/+1 counter on it.")
    }
}
