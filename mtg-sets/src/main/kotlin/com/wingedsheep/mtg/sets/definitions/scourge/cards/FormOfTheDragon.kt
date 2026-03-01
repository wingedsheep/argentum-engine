package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeAttackedWithout
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect

/**
 * Form of the Dragon
 * {4}{R}{R}{R}
 * Enchantment
 * At the beginning of your upkeep, this enchantment deals 5 damage to any target.
 * At the beginning of each end step, your life total becomes 5.
 * Creatures without flying can't attack you.
 */
val FormOfTheDragon = card("Form of the Dragon") {
    manaCost = "{4}{R}{R}{R}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, this enchantment deals 5 damage to any target.\n" +
        "At the beginning of each end step, your life total becomes 5.\n" +
        "Creatures without flying can't attack you."

    // At the beginning of your upkeep, this enchantment deals 5 damage to any target.
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        val t = target("any target", Targets.Any)
        effect = DealDamageEffect(5, t)
    }

    // At the beginning of each end step, your life total becomes 5.
    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = SetLifeTotalEffect(5)
    }

    // Creatures without flying can't attack you.
    staticAbility {
        ability = CantBeAttackedWithout(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2058bcb4-50ac-4323-ab49-3b80a5891894.jpg?1562526178"
        ruling("2004-10-04", "If your life total was above 5 at end of turn, then you lose life to make your total 5. If it was less than 5, you gain life to bring it to 5.")
        ruling("2004-10-04", "It sets your life total at the beginning of the end step of every player's turn, not just your own.")
        ruling("2014-02-01", "Unless some effect explicitly says otherwise, a creature that can't attack you can still attack a planeswalker you control.")
    }
}
