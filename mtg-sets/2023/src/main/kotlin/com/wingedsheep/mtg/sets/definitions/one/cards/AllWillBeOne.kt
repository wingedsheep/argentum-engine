package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * All Will Be One
 * {3}{R}{R}
 * Enchantment
 *
 * Whenever you put one or more counters on a permanent or player, this enchantment deals that much
 * damage to target opponent, creature an opponent controls, or planeswalker an opponent controls.
 *
 * Fires once per recipient per placement (proliferating three permanents is three triggers), with
 * "that much" the counters that recipient got — toxic creatures hitting one player together are one
 * event and one trigger.
 */
val AllWillBeOne = card("All Will Be One") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you put one or more counters on a permanent or player, this enchantment deals " +
        "that much damage to target opponent, creature an opponent controls, or planeswalker an opponent controls."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent).getsCounters(by = Player.You, orPlayer = true)
        val t = target(Targets.OpponentOrTheirCreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.triggerCountersPlaced(), t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "118"
        artist = "Chris Rahn"
        flavorText = "The Invasion Tree broke through the Blind Eternities and sent Phyrexian perfection " +
            "coursing across the Multiverse."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d75e1f4-bd63-428e-8e6e-131594b3ba44.jpg?1783918036"
    }
}
