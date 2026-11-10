package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Olivia, Mobilized for War — Shadows over Innistrad #248
 * {1}{B}{R}
 * Legendary Creature — Vampire Knight
 * 3/3
 * Flying
 * Whenever another creature you control enters, you may discard a card. If you do, put a +1/+1
 * counter on that creature, it gains haste until end of turn, and it becomes a Vampire in addition
 * to its other types.
 *
 * - The discard is decided on resolution; if the entering creature has already left the
 *   battlefield you may still discard, and the rest does nothing (the effects on the triggering
 *   entity find no permanent).
 * - Only haste ends at end of turn; the counter and the Vampire subtype stay (Duration.Permanent).
 */
val OliviaMobilizedForWar = card("Olivia, Mobilized for War") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Vampire Knight"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever another creature you control enters, you may discard a card. If you do, put a +1/+1 " +
        "counter on that creature, it gains haste until end of turn, and it becomes a Vampire in " +
        "addition to its other types."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity, Duration.EndOfTurn) then
                    Effects.AddCreatureType("Vampire", EffectTarget.TriggeringEntity, Duration.Permanent)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "248"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7297dd81-8367-42e9-abf9-9227e202e7ae.jpg?1783937713"
        ruling(
            "2016-04-08",
            "You decide whether to discard a card as the triggered ability of Olivia, Mobilized for War " +
                "resolves. If you do, the rest of the ability's effects happen before any player may take actions."
        )
        ruling(
            "2016-04-08",
            "If the entering creature leaves the battlefield before the triggered ability of Olivia, " +
                "Mobilized for War resolves, you may still discard a card. The rest of the ability has no effect."
        )
        ruling(
            "2016-04-08",
            "If you discard a card, the creature only gains haste until end of turn, but the +1/+1 counter " +
                "and Vampire subtype remain indefinitely."
        )
    }
}
