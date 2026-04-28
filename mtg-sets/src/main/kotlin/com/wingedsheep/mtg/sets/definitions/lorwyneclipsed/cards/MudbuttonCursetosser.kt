package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mudbutton Cursetosser
 * {B}
 * Creature — Goblin Warlock
 * 2/1
 *
 * As an additional cost to cast this spell, behold a Goblin or pay {2}.
 * (To behold a Goblin, choose a Goblin you control or reveal a Goblin card from your hand.)
 * This creature can't block.
 * When this creature dies, destroy target creature an opponent controls with power 2 or less.
 */
val MudbuttonCursetosser = card("Mudbutton Cursetosser") {
    manaCost = "{B}"
    typeLine = "Creature — Goblin Warlock"
    power = 2
    toughness = 1
    oracleText = "As an additional cost to cast this spell, behold a Goblin or pay {2}. " +
        "(To behold a Goblin, choose a Goblin you control or reveal a Goblin card from your hand.)\n" +
        "This creature can't block.\n" +
        "When this creature dies, destroy target creature an opponent controls with power 2 or less."

    additionalCost(
        AdditionalCost.BeholdOrPay(
            filter = Filters.WithSubtype("Goblin"),
            alternativeManaCost = "{2}"
        )
    )

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.Dies
        val creature = target(
            "creature an opponent controls with power 2 or less",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.opponentControls().powerAtMost(2)))
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Ioannis Fiore"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35bc841a-9a21-4c17-a60a-a3ee01472fcb.jpg?1767871878"
    }
}
