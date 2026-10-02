package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

// Modern Oracle wording removes the old blocker before optionally making it block anew.
val FalseOrders = card("False Orders") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Cast this spell only during the declare blockers step.\nRemove target creature defending player controls from combat. Creatures it was blocking that had become blocked by only that creature this combat become unblocked. You may have it block an attacking creature of your choice."
    spell {
        castOnlyDuring(Step.DECLARE_BLOCKERS)
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.RemoveFromCombat(creature, unblockSoleBlockedAttackers = true) then
            Effects.Pipeline {
                val attackers = gather(CardSource.BattlefieldMatching(GameObjectFilter.Creature.attackingDefenderOf(creature)))
                val chosen = chooseUpTo(
                    1, attackers,
                    prompt = "Choose an attacking creature for the creature to block, or choose none.",
                    useTargetingUI = true
                )
                run(Effects.BecomeBlocking(creature, chosen.asTarget))
            }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eb71ac4-796d-4011-9002-1129bc09c284.jpg?1783948687"
        ruling("2004-10-04", "If a creature is removed from being a blocker of a given attacker, any triggered abilities that would have happened because it was declared as a blocker still happen.")
    }
}
