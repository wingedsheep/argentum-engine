package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Daxos, Blessed by the Sun
 * {W}{W}
 * Legendary Enchantment Creature — Demigod
 * 2/★
 * Daxos's toughness is equal to your devotion to white.
 * Whenever another creature you control enters or dies, you gain 1 life.
 *
 * The toughness is a characteristic-defining ability ([dynamicToughness], People of the Woods'
 * shape) over [DynamicAmounts.devotionTo] white — Daxos's own {W}{W} counts while it is on the
 * battlefield. The life trigger is one ability with two event atoms joined by [Triggers.or]
 * (Slagstone Refinery's `another(…)` disjunction), so each creature entering or dying gains 1.
 */
val DaxosBlessedByTheSun = card("Daxos, Blessed by the Sun") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Creature — Demigod"
    oracleText = "Daxos's toughness is equal to your devotion to white. (Each {W} in the mana costs " +
        "of permanents you control counts toward your devotion to white.)\n" +
        "Whenever another creature you control enters or dies, you gain 1 life."

    power = 2
    dynamicToughness(DynamicAmounts.devotionTo(Color.WHITE))

    triggeredAbility {
        val otherCreature = Triggers.another(GameObjectFilter.Creature.youControl())
        trigger = Triggers.or(otherCreature.enters(), otherCreature.dies())
        effect = Effects.GainLife(1)
        description = "Whenever another creature you control enters or dies, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd2adb3-a6f4-448e-af2a-15ca92b1de63.jpg?1783931602"
        ruling("2020-01-24", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to Daxos may become lethal if your devotion to white decreases during that turn.")
        ruling("2020-01-24", "If another creature you control enters the battlefield at the same time as Daxos, you gain 1 life for that creature. Similarly, if another creature you control dies at the same time as Daxos, you gain 1 life for that creature. However, in the unusual case that Daxos dies at the same time that another creature enters the battlefield or vice versa, you won't gain 1 life for that creature.")
        ruling("2020-01-24", "The ability that defines Daxos's toughness works in all zones, not just the battlefield.")
    }
}
