package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Provoke the Trolls
 * {3}{R}
 * Instant
 * Provoke the Trolls deals 3 damage to any target. If a creature is dealt damage this way, it gets
 * +5/+0 until end of turn.
 *
 * Silver Bolt's shape: the damage instruction records the damage it really dealt
 * (`damageDealtVariable`, after prevention and redirection), and the pump needs both that to be at
 * least 1 and the target to be a creature — a player or noncreature planeswalker gets nothing, and
 * neither does a creature whose damage was prevented. The creature check reads the target after the
 * damage, still on the battlefield (no state-based-action pass runs mid-resolution).
 */
val ProvokeTheTrolls = card("Provoke the Trolls") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Provoke the Trolls deals 3 damage to any target. If a creature is dealt damage this way, " +
        "it gets +5/+0 until end of turn."

    spell {
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber { Effects.DealDamage(3, t, damageDealtVariable = it) }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GTE, 1),
                        Conditions.TargetMatchesFilter(GameObjectFilter.Creature, t)
                    ),
                    then = Effects.ModifyStats(5, 0, t)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Deruchenko Alexander"
        flavorText = "\"I like burning and pillaging as much as anyone, but sometimes it's just easier to " +
            "throw a rock and let the trolls do the work.\"\n—Urik, Tuskeri raid leader"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2727b05a-0c86-4c59-b7b4-425bdd8e775d.jpg?1783928226"
    }
}
