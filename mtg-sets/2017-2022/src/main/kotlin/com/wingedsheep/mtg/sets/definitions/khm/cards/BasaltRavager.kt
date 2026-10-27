package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Basalt Ravager
 * {3}{R}
 * Creature — Giant Wizard
 * 4/2
 *
 * When this creature enters, it deals X damage to any target, where X is the greatest number of
 * creatures you control that have a creature type in common.
 *
 * X is [DynamicAmounts.largestSharedCreatureTypeCount] — the largest creature-type tribe among your
 * creatures, read from projected state as the trigger resolves (so it counts the Ravager itself if
 * it's still yours, and no creature type is chosen). The target is locked in when the trigger goes
 * on the stack; only the amount waits for resolution.
 */
val BasaltRavager = card("Basalt Ravager") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Wizard"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, it deals X damage to any target, where X is the greatest " +
        "number of creatures you control that have a creature type in common."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.largestSharedCreatureTypeCount(Player.You), t)
        description = "When this creature enters, it deals X damage to any target, where X is the " +
            "greatest number of creatures you control that have a creature type in common."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b32aea04-04f7-48a8-a8ab-8b38fa53da3b.jpg?1783928234"
        ruling(
            "2021-02-05",
            "The amount of damage dealt is calculated as the triggered ability resolves based on the " +
                "creatures you control at that time, including Basalt Ravager itself if it's still under " +
                "your control. You'll always use the greatest number possible. You don't need to choose " +
                "any creature types."
        )
    }
}
