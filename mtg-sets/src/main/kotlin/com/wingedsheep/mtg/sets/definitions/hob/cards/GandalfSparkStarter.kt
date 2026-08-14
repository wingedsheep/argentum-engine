package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Gandalf, Spark Starter — The Hobbit #97
 * {4}{R}{R} · Legendary Creature — Avatar Wizard · Uncommon
 * 4/3
 *
 * Reach
 * When Gandalf enters, he deals 3 damage divided as you choose among one, two, or three targets.
 *
 * Modeling notes:
 *  - The division is announced as the trigger goes on the stack, alongside the targets — that's
 *    what [AnyTarget] (one to three) plus [DividedDamageEffect] models (cf. Twin Bolt, Arc
 *    Lightning). Each chosen target must be assigned at least 1 damage, so three targets means
 *    exactly 1 each.
 *  - "Any target" is creature / player / planeswalker / battle, not just creatures.
 */
val GandalfSparkStarter = card("Gandalf, Spark Starter") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 4
    toughness = 3
    oracleText = "Reach\n" +
        "When Gandalf enters, he deals 3 damage divided as you choose among one, two, or three targets."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = AnyTarget(count = 3, minCount = 1)
        effect = DividedDamageEffect(
            totalDamage = 3,
            minTargets = 1,
            maxTargets = 3
        )
        description = "When Gandalf, Spark Starter enters, he deals 3 damage divided as you choose " +
            "among one, two, or three targets."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Javier Charro"
        flavorText = "Gandalf thought of most things; and though he could not do everything, he could " +
            "do a great deal for friends in a tight corner."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c5c6f1c-35cf-4172-b5a1-b73222b0723b.jpg?1784895032"
    }
}
