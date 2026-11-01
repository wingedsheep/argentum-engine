package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreadhorde Butcher — War of the Spark #194 (canonical printing)
 * {B}{R}
 * Creature — Zombie Warrior
 * 1/1
 * Haste
 * Whenever this creature deals combat damage to a player or planeswalker, put a +1/+1 counter on this creature.
 * When this creature dies, it deals damage equal to its power to any target.
 *
 * The dies trigger reads the Butcher's last-known power (counters included) via
 * `DynamicAmounts.sourcePower()`, the same shape as Heartfire Hero.
 */
val DreadhordeButcher = card("Dreadhorde Butcher") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Zombie Warrior"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Whenever this creature deals combat damage to a player or planeswalker, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, it deals damage equal to its power to any target."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/457bc402-4c31-465c-95e1-694f686e257e.jpg?1783933398"
        ruling("2019-05-03", "Dreadhorde Butcher's last known existence on the battlefield is checked to determine its power as its last ability resolves. If its power was 0 or less, the ability resolves with no effect.")
    }
}
