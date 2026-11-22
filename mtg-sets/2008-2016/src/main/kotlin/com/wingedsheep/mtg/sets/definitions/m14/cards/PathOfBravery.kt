package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Path of Bravery
 * {2}{W}
 * Enchantment
 *
 * As long as your life total is greater than or equal to your starting life total, creatures you
 * control get +1/+1.
 * Whenever one or more creatures you control attack, you gain life equal to the number of
 * attacking creatures.
 *
 * - "Greater than or equal to" is [Conditions.LifeAboveStartingBy] with a margin of 0 — it
 *   compares against the seat's real starting life total (20 / 30 / 40), never a hardcoded 20.
 * - The attack trigger fires once per declaration; the count is read as it resolves, so a
 *   creature put onto the battlefield attacking before then is counted (2017-11-17 ruling).
 */
val PathOfBravery = card("Path of Bravery") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As long as your life total is greater than or equal to your starting life total, creatures you control get +1/+1.\n" +
        "Whenever one or more creatures you control attack, you gain life equal to the number of attacking creatures."

    staticAbility {
        condition = Conditions.LifeAboveStartingBy(0)
        ability = ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5222e200-df1b-46c6-a194-c341e8c1d516.jpg?1783939941"
        ruling(
            "2017-11-17",
            "If a creature you control is dealt damage at the same time that you gain life (most likely because a " +
                "source you control with lifelink deals damage at the same time), and your life total becomes greater " +
                "than or equal to your starting life total, apply Path of Bravery's effect before considering whether " +
                "or not the damage dealt to that creature is lethal."
        )
        ruling(
            "2017-11-17",
            "Path of Bravery's triggered ability triggers only once, after attacking creatures are declared. For " +
                "example, if you attack with five creatures, you'll gain 5 life as a single event."
        )
        ruling(
            "2017-11-17",
            "If a creature enters the battlefield attacking after attacking creatures have been declared, the ability " +
                "won't trigger again because you didn't declare that creature as an attacking creature. However, if such " +
                "a creature enters the battlefield before the ability resolves, it will be counted when determining how " +
                "much life is gained."
        )
    }
}
