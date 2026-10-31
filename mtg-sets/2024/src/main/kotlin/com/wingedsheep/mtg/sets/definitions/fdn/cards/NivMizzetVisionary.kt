package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Niv-Mizzet, Visionary
 * {4}{U}{R}
 * Legendary Creature — Dragon Wizard
 * 5/5
 * Flying
 * You have no maximum hand size.
 * Whenever a source you control deals noncombat damage to an opponent, you draw that many cards.
 *
 * "A source you control" is modelled the same way as Twinflame Tyrant — a `DealsDamageEvent`
 * whose `sourceFilter` is `GameObjectFilter.Any.youControl()`, restricted here to noncombat
 * damage aimed at an opponent. The draw count reads the triggering damage amount via
 * [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] ("draw that many cards").
 */
val NivMizzetVisionary = card("Niv-Mizzet, Visionary") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Dragon Wizard"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "You have no maximum hand size.\n" +
        "Whenever a source you control deals noncombat damage to an opponent, you draw that many cards."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = NoMaximumHandSize()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "123"
        artist = "Dan Murayama Scott"
        flavorText = "Ravnica's sharpest mind and its biggest ego."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a69a618-d588-4745-8ede-0ff0a9f356f1.jpg?1782689159"
    }
}
