package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Arlinn, Voice of the Pack — War of the Spark #150 (canonical printing)
 * {4}{G}{G}
 * Legendary Planeswalker — Arlinn
 * Starting Loyalty: 7
 *
 * Each creature you control that's a Wolf or a Werewolf enters with an additional +1/+1 counter on it.
 * −2: Create a 2/2 green Wolf creature token.
 *
 * The static is the Grumgully shape: an [EntersWithDynamicCounters] replacement scoped to Wolf-or-
 * Werewolf creatures you control, with `otherOnly` routing it through the global entry sweep. Being a single replacement, a creature that's both types gets
 * exactly one counter; it is active only on the battlefield, so the −2's Wolf gets no counter if
 * Arlinn has left by the time the ability resolves.
 */
val ArlinnVoiceOfThePack = card("Arlinn, Voice of the Pack") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Arlinn"
    startingLoyalty = 7
    oracleText = "Each creature you control that's a Wolf or a Werewolf enters with an additional +1/+1 counter on it.\n" +
        "−2: Create a 2/2 green Wolf creature token."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.fixed(1),
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf").youControl(),
                to = Zone.BATTLEFIELD,
            ),
            otherOnly = true,
        )
    )

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            imageUri = "https://cards.scryfall.io/normal/front/5/5/551f3219-3e98-4354-abcd-db22c3253105.jpg?1783933347"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5391d2f1-e8d3-4d39-98cf-367888e10534.jpg?1783933416"

        ruling("2019-05-03", "A creature that's a Wolf or Werewolf enters the battlefield with one +1/+1 counter if it would otherwise enter with no +1/+1 counters.")
        ruling("2019-05-03", "A creature that's both a Wolf and a Werewolf receives only one additional +1/+1 counter from Arlinn's ability.")
        ruling("2019-05-03", "If Arlinn leaves the battlefield before her last ability resolves, most likely because she only had 2 loyalty when you activated the ability, the Wolf token won't enter the battlefield with a +1/+1 counter.")
    }
}
