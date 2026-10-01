package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cacophony Scamp
 * {R}
 * Creature — Phyrexian Goblin Warrior
 * 1/1
 *
 * Whenever this creature deals combat damage to a player, you may sacrifice it. If you do,
 * proliferate.
 * When this creature dies, it deals damage equal to its power to any target.
 *
 * Current Oracle wording is "If you do" (the printed card said "When you do"), so the
 * proliferate is part of the same ability: an optional trigger whose `IfYouDo` gate is keyed on
 * a permanent actually being sacrificed — if the Scamp has already left the battlefield, nothing
 * is sacrificed and there is no proliferate. Sacrificing it also fires its own dies trigger,
 * which reads the Scamp's last-known power.
 */
val CacophonyScamp = card("Cacophony Scamp") {
    manaCost = "{R}"
    typeLine = "Creature — Phyrexian Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, you may sacrifice it. If you do, " +
        "proliferate. (Choose any number of permanents and/or players, then give each another counter of " +
        "each kind already there.)\nWhen this creature dies, it deals damage equal to its power to any target."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            then = Effects.Proliferate(),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "Whenever this creature deals combat damage to a player, you may sacrifice it. " +
            "If you do, proliferate."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), t)
        description = "When this creature dies, it deals damage equal to its power to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3553dcef-7b99-49d5-b071-06b894696952.jpg?1783918034"
        ruling(
            "2023-02-04",
            "You choose whether to sacrifice Cacophony Scamp as its first ability resolves. No player may " +
                "respond between the time you sacrifice it and the time you proliferate."
        )
    }
}
