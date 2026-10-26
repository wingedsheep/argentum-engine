package com.wingedsheep.mtg.sets.definitions.afc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Dragonborn Champion
 * {2}{R}{G}
 * Creature — Dragon Warrior
 * 5/3
 * Trample
 * Whenever a source you control deals 5 or more damage to a player, draw a card.
 *
 * Any source you control (combat or not) dealing damage to a player is one damage event per
 * source and recipient; the "5 or more" is a `triggerRestriction` on that event's amount — a
 * condition of the trigger event itself, not an intervening "if". Per the ruling, 3 then 2 from
 * the same source at different times never adds up, which the per-event amount guarantees.
 */
val DragonbornChampion = card("Dragonborn Champion") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dragon Warrior"
    power = 5
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever a source you control deals 5 or more damage to a player, draw a card."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.AnyPlayer)
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggerDamageAmount(),
            ComparisonOperator.GTE,
            5,
        )
        effect = Effects.DrawCards(1)
        description = "Whenever a source you control deals 5 or more damage to a player, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"I was raised on the stories of my ancestors, and I want to write my own pages in the tales.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f6e43a7-84b9-4e89-bfe1-442d08df34d8.jpg?1783926240"
        ruling(
            "2021-07-23",
            "Dragonborn Champion's last ability only triggers if 5 or more damage is dealt by the same " +
                "source at the same time. If a source deals 3 damage to a player and then later deals 2 " +
                "more damage to the same player, this will not cause the ability to trigger."
        )
    }
}
