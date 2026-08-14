package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Realm-Scorcher Hellkite
 * {4}{R}{R}
 * Creature — Dragon
 * 4/6
 *
 * Bargain
 * Flying, haste
 * When this creature enters, if it was bargained, add four mana in any combination of colors.
 * {1}{R}: This creature deals 1 damage to any target.
 *
 * The bargain condition is an intervening "if": an unbargained Hellkite never puts the mana
 * ability on the stack. The four mana is distributed pip by pip, so the player may choose a mix
 * of colors rather than one color for all four.
 */
val RealmScorcherHellkite = card("Realm-Scorcher Hellkite") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 6
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Flying, haste\n" +
        "When this creature enters, if it was bargained, add four mana in any combination of " +
        "colors.\n" +
        "{1}{R}: This creature deals 1 damage to any target."

    keywords(Keyword.FLYING, Keyword.HASTE)
    bargain()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        effect = Effects.AddManaInAnyCombination(4)
        description = "When this creature enters, if it was bargained, add four mana in any " +
            "combination of colors."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        val target = target("any target", Targets.Any)
        effect = Effects.DealDamage(1, target)
        description = "This creature deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/845b3c26-05da-4a09-a6c9-4ea4166104a7.jpg?1783915090"
    }
}
