package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Murk Dwellers
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * Whenever this creature attacks and isn't blocked, it gets +2/+0 until end of combat.
 *
 * Composes existing primitives: [Triggers.AttacksAndIsntBlocked] (which fires in the declare
 * blockers step once no creature has been declared to block it) driving a self-targeted
 * [Effects.ModifyStats] for [Duration.EndOfCombat]. Same shape as Arabian Nights' Merchant Ship.
 */
val MurkDwellers = card("Murk Dwellers") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks and isn't blocked, it gets +2/+0 until end of combat."

    triggeredAbility {
        trigger = Triggers.AttacksAndIsntBlocked
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self, Duration.EndOfCombat)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Drew Tucker"
        flavorText = "When Raganorn unsealed the catacombs, he found more than the dead and their treasures."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a213450f-02f4-4c08-8da8-891ebfa8e237.jpg?1783947938"
        ruling(
            "2013-04-15",
            "An ability that triggers when something \"attacks and isn't blocked\" triggers in the " +
                "declare blockers step after blockers are declared if (1) that creature is attacking " +
                "and (2) no creatures are declared to block it. It will trigger even if that creature " +
                "was put onto the battlefield attacking rather than having been declared as an " +
                "attacker in the declare attackers step."
        )
    }
}
