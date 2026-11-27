package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Druids' Repository
 * {1}{G}{G}
 * Enchantment
 *
 * Whenever a creature you control attacks, put a charge counter on this enchantment.
 * Remove a charge counter from this enchantment: Add one mana of any color.
 *
 * - The trigger fires once per attacking creature you control (ruling), the Brutal Hordechief shape.
 * - The activation costs only [Costs.RemoveCounterFromSelf], so it is a true mana ability
 *   (`manaAbility = true` + [TimingRule.ManaAbility]): no stack, can't be responded to, and can be
 *   activated as many times as there are charge counters (the Morselhoarder idiom).
 */
val DruidsRepository = card("Druids' Repository") {
    manaCost = "{1}{G}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, put a charge counter on this enchantment.\n" +
        "Remove a charge counter from this enchantment: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CHARGE)
        effect = Effects.AddManaOfChoice(ManaColorSet.AnyColor, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57e6fb62-7ee3-444d-8fd4-c1f44014a05c.jpg?1783940668"
        ruling("2012-05-01", "You can activate the second ability of Druids' Repository multiple times in the same turn. It's a mana ability, doesn't use the stack, and can't be responded to.")
        ruling("2012-05-01", "The first ability of Druids' Repository will trigger once for each creature you control that attacks.")
    }
}
