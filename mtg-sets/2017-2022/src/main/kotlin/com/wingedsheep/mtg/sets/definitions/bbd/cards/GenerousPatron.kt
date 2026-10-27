package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Generous Patron
 * {2}{G}
 * Creature — Elf Advisor
 * 1/4
 * When this creature enters, support 2. (Put a +1/+1 counter on each of up to two other target creatures.)
 * Whenever you put one or more counters on a creature you don't control, draw a card.
 */
val GenerousPatron = card("Generous Patron") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Advisor"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, support 2. (Put a +1/+1 counter on each of up to two " +
        "other target creatures.)\n" +
        "Whenever you put one or more counters on a creature you don't control, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 2 — put a +1/+1 counter on each of up " +
            "to two other target creatures."
    }

    // Any counter kind, placed by you, on an opponent's creature — once per such creature (the
    // per-recipient template), so supporting two opposing creatures draws two cards.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).getsCounters(by = Player.You)
        effect = Effects.DrawCards(1)
        description = "Whenever you put one or more counters on a creature you don't control, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59ea095b-af0e-4178-b6f4-816abb635fc0.jpg?1783934853"
        ruling("2018-06-08", "If you put one or more counters on multiple creatures you don't control at the same time, such as by supporting two different creatures you don't control, Generous Patron's last ability triggers for each of those creatures.")
        ruling("2018-06-08", "Support can target a creature another player controls.")
        ruling("2018-06-08", "If a creature enters the battlefield with counters under another player's control, that player is the player who puts those counters on it, even if you control the spell or ability putting that creature onto the battlefield.")
        ruling("2018-06-08", "Generous Patron's last ability triggers even if the counters you put on another player's creatures aren't +1/+1 counters.")
    }
}
