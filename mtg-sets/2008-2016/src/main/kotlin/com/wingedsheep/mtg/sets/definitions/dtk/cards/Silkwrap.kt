package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silkwrap
 * {1}{W}
 * Enchantment
 * When this enchantment enters, exile target creature with mana value 3 or less an opponent
 * controls until this enchantment leaves the battlefield.
 *
 * Banishing Light's two-trigger shape: the enters trigger links the exiled card, the leaves
 * trigger returns everything linked under its owner's control.
 */
val Silkwrap = card("Silkwrap") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature with mana value 3 or less an opponent controls " +
        "until this enchantment leaves the battlefield. (That creature returns under its owner's control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.manaValueAtMost(3).opponentControls())
        effect = Effects.ExileUntilLeaves(creature)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "David Gaillet"
        flavorText = "Better scarves than scars."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68ee9009-59a7-40e6-bf1a-1d26d4b399e8.jpg"
    }
}
