package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sparktongue Dragon
 * {3}{R}{R}
 * Creature — Dragon
 * 3/3
 * Flying
 * When this creature enters, you may pay {2}{R}. When you do, it deals 3 damage to any target.
 *
 * "You may pay {2}{R}. When you do, …" is a reflexive triggered ability (CR 603.12): the optional
 * payment happens while the enters trigger resolves, and only then does the damage ability go on
 * the stack, with its target chosen at that point.
 */
val SparktongueDragon = card("Sparktongue Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, you may pay {2}{R}. When you do, it deals 3 damage to any target."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{2}{R}"),
            optional = true,
            descriptionOverride = "it deals 3 damage to any target",
        ) {
            val anyTarget = target(Targets.Any)
            effect = Effects.DealDamage(
                amount = 3,
                target = anyTarget,
                damageSource = EffectTarget.Self,
            )
        }
        description = "When this creature enters, you may pay {2}{R}. When you do, it deals 3 damage " +
            "to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Daarken"
        flavorText = "Fools believe that Kolaghan's brood follows lightning storms. The wise know that " +
            "it is the other way around."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc0dbc86-cc12-49e7-9721-dd52b35efcbe.jpg?1783934547"
        ruling(
            "2020-11-10",
            "You don't choose a target for Sparktongue Dragon's last ability at the time it triggers. " +
                "Rather, a second \"reflexive\" ability triggers when you pay {2}{R} this way. You choose a " +
                "target for that ability as it goes on the stack. Each player may respond to this " +
                "triggered ability as normal."
        )
    }
}
