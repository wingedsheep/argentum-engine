package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Baffling End (RIX #1)
 * {1}{W}  Enchantment
 *
 * When this enchantment enters, exile target creature an opponent controls with mana value 3 or less.
 * When this enchantment leaves the battlefield, target opponent creates a 3/3 green Dinosaur
 * creature token with trample.
 *
 * The exile is permanent — not an "until this leaves" O-Ring — and the two abilities are
 * independent (rulings): the Dinosaur arrives even if the first trigger fizzled, and in multiplayer
 * the opponent who gets it needn't be the one whose creature was exiled.
 */
val BafflingEnd = card("Baffling End") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature an opponent controls with mana value 3 or less.\n" +
        "When this enchantment leaves the battlefield, target opponent creates a 3/3 green Dinosaur creature token with trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtMost(3)))
        effect = Effects.Exile(creature)
        description = "When this enchantment enters, exile target creature an opponent controls with mana value 3 or less."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Dinosaur"),
            keywords = setOf(Keyword.TRAMPLE),
            controller = opponent,
        )
        description = "When this enchantment leaves the battlefield, target opponent creates a 3/3 green Dinosaur creature token with trample."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/208c6f51-3c00-4fc6-8579-8f57444d0e97.jpg?1783935341"
        ruling(
            "2018-01-19",
            "While Baffling End's two abilities are flavorfully related, they are independent. If the first " +
                "ability doesn't resolve (perhaps because the target gained hexproof), the second will still " +
                "create a Dinosaur token when Baffling End leaves the battlefield.",
        )
        ruling(
            "2018-01-19",
            "In a multiplayer game, the target opponent who creates a Dinosaur token doesn't have to be the " +
                "same one whose creature was exiled.",
        )
    }
}
