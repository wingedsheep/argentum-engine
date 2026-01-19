package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AssignDamageEqualToToughness
import com.wingedsheep.rulesengine.ability.ModifyStats
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost

object BarkOfDoran {
    val definition = CardDefinition.equipment(
        name = "Bark of Doran",
        manaCost = ManaCost.parse("{1}{W}"),
        equipCost = ManaCost.parse("{1}"),
        oracleText = "Equipped creature gets +0/+1.\n" +
                "As long as equipped creature's toughness is greater than its power, " +
                "it assigns combat damage equal to its toughness rather than its power.\n" +
                "Equip {1}"
    )

    val script = cardScript("Bark of Doran") {
        // Static ability: +0/+1 to equipped creature
        modifyStats(power = 0, toughness = 1, target = StaticTarget.AttachedCreature)

        // Static ability: Toughness-based damage when toughness > power
        staticAbility(AssignDamageEqualToToughness(StaticTarget.AttachedCreature))
    }
}