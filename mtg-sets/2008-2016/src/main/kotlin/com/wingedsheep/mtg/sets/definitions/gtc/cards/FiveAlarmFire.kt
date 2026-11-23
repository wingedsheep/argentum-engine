package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Five-Alarm Fire
 * {1}{R}{R}
 * Enchantment
 *
 * The counter trigger is one observer trigger over creatures you control. Combat damage from one
 * creature to several recipients (a trampler hitting its blocker and the player) is folded into a
 * single trigger per source, so it adds one counter; a double striker deals combat damage in two
 * steps and adds two (2013-01-24 ruling).
 */
val FiveAlarmFire = card("Five-Alarm Fire") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control deals combat damage, put a blaze counter on this enchantment.\n" +
        "Remove five blaze counters from this enchantment: It deals 5 damage to any target."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage()
        effect = Effects.AddCounters(CounterType.BLAZE, 1, EffectTarget.Self)
        description = "Whenever a creature you control deals combat damage, put a blaze counter on this enchantment."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.BLAZE, 5)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(5, anyTarget)
        description = "Remove five blaze counters from this enchantment: It deals 5 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb550d0c-2261-4f41-a2b1-d185f0bce86a.jpg?1783940125"
        ruling("2013-01-24", "Five-Alarm Fire's first ability triggers once for each time a creature you control deals combat damage, even if it deals combat damage to more than one creature, planeswalker, or player. For example, an attacking creature with trample that deals combat damage to a blocking creature and the defending player will cause Five-Alarm Fire's ability to trigger once. A creature with double strike, however, may cause Five-Alarm Fire's ability to trigger twice during a single combat.")
    }
}
