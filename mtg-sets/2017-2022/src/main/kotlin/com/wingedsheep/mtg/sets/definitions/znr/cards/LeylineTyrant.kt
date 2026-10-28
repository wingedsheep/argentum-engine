package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RetainUnspentColoredMana

/**
 * Leyline Tyrant — Zendikar Rising #147
 * {2}{R}{R} · Creature — Dragon · Mythic · 4/4
 *
 * Flying
 * You don't lose unspent red mana as steps and phases end.
 * When this creature dies, you may pay any amount of {R}. When you do, it deals that much damage
 * to any target.
 *
 * Modeling notes:
 *  - Mana retention is the colour-filtered `RetainUnspentColoredMana(Color.RED)` static (Electro,
 *    Assaulting Battery): only red survives the step/phase boundary.
 *  - "Pay any amount of {R}" is `Effects.PayRepeatedly("{R}")` with no cap — the count offered is
 *    bounded by the *red* mana the controller can actually produce (floating retained red first),
 *    never by generic mana. Wrapped in an optional `ReflexiveTrigger` for the CR 603.12
 *    "when you do": the dies trigger itself has no target, and the damage half goes on the stack as
 *    a second ability, targeted then, reading the paid count as `DynamicAmounts.timesPaid()`.
 *  - "It deals" is the Tyrant — the reflexive ability's source, read through last-known information.
 */
val LeylineTyrant = card("Leyline Tyrant") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "You don't lose unspent red mana as steps and phases end.\n" +
        "When this creature dies, you may pay any amount of {R}. When you do, it deals that much " +
        "damage to any target."

    keywords(Keyword.FLYING)

    // You don't lose unspent red mana as steps and phases end.
    staticAbility {
        ability = RetainUnspentColoredMana(Color.RED)
    }

    // When this creature dies, you may pay any amount of {R}. When you do, it deals that much
    // damage to any target.
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{R}"),
            optional = true,
            descriptionOverride = "You may pay any amount of {R}. When you do, Leyline Tyrant deals " +
                "that much damage to any target."
        ) {
            val target = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.timesPaid(), target)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "147"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/847ae75b-1c0f-4d0a-9933-9a302c88dc27.jpg?1783929358"
        ruling(
            "2020-09-25",
            "You can keep unspent red mana indefinitely while Leyline Tyrant is on the battlefield. Other " +
                "types of mana will continue to be lost as normal as each step and phase ends."
        )
        ruling(
            "2020-09-25",
            "Once Leyline Tyrant leaves the battlefield, you have until the end of the current step or phase " +
                "to spend your unspent red mana before it is lost as normal."
        )
        ruling(
            "2020-09-25",
            "Leyline Tyrant's last ability goes on the stack without a target. While that ability is " +
                "resolving, you may pay any amount of red mana, including from the unspent mana you have. If " +
                "you do, a second ability triggers and you pick one target to be dealt that much damage."
        )
    }
}
