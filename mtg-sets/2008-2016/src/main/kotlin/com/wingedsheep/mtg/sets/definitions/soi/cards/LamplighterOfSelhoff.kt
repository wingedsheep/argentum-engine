package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lamplighter of Selhoff (Shadows over Innistrad #72)
 * {4}{U}
 * Creature — Zombie Horror
 * 3/5
 *
 * When this creature enters, if you control another Zombie, you may draw a card. If you do,
 * discard a card.
 *
 * - Intervening "if": checked when the trigger would go on the stack and again on resolution.
 *   "Another Zombie" is any other Zombie permanent you control — the Lamplighter itself is excluded.
 */
val LamplighterOfSelhoff = card("Lamplighter of Selhoff") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    oracleText = "When this creature enters, if you control another Zombie, you may draw a card. " +
        "If you do, discard a card."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE),
            excludeSelf = true
        )
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Clint Cearley"
        flavorText = "For stitchers and necro-alchemists, it's important that work can continue at all hours."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80dadecc-f1f1-44a2-8829-6c94aade73a0.jpg?1783937795"
    }
}
