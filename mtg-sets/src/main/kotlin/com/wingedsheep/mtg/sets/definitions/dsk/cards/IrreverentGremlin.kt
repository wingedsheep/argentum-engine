package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Irreverent Gremlin — Duskmourn: House of Horror #142
 * {1}{R}
 * Creature — Gremlin
 * 2/2
 *
 * Menace
 * Whenever another creature you control with power 2 or less enters, you may discard a card.
 * If you do, draw a card. Do this only once each turn.
 *
 * The "rummage" is the standard MayEffect(IfYouDoEffect(discard, draw)) idiom — declining (or an
 * empty hand) skips the draw. "Do this only once each turn" is the trigger-level [oncePerTurn]
 * cap (CR 603.3b): the ability triggers at most once per turn even if several qualifying creatures
 * enter.
 */
val IrreverentGremlin = card("Irreverent Gremlin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Gremlin"
    power = 2
    toughness = 2
    oracleText = "Menace\nWhenever another creature you control with power 2 or less enters, " +
        "you may discard a card. If you do, draw a card. Do this only once each turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtMost(2),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(1)
            ),
            descriptionOverride = "You may discard a card. If you do, draw a card."
        )
        oncePerTurn = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8da254f5-53f2-41d2-a4f0-a90b3dd6209c.jpg?1726286376"
    }
}
