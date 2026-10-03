package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smelted Chargebug
 * {1}{R}
 * Artifact Creature — Insect
 * 1/3
 *
 * Menace
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}. If you do, another target attacking creature
 * gets +1/+0 and gains menace until end of turn.
 *
 * An "if you do": the target is chosen as the trigger goes on the stack; the {E} payment is offered
 * on resolution via [Effects.MayPay], only when the player has an energy counter to pay.
 */
val SmeltedChargebug = card("Smelted Chargebug") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Insect"
    power = 1
    toughness = 3
    oracleText = "Menace\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}. If you do, another target attacking creature " +
        "gets +1/+0 and gains menace until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter(GameObjectFilter.Creature.attacking(), excludeSelf = true))
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.ModifyStats(1, 0, creature) then Effects.GrantKeyword(Keyword.MENACE, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dad40bf-2cd0-47f5-a878-9a289d1d58d0.jpg?1783911266"
    }
}
