package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Squirrel Sanctuary
 * {G}
 * Enchantment
 * When this enchantment enters, create a 1/1 green Squirrel creature token.
 * Whenever a nontoken creature you control dies, you may pay {1}. If you do, return this
 * enchantment to its owner's hand.
 *
 * The bounce is guarded with `fromZone = BATTLEFIELD`: per the 2021-06-18 ruling the last ability
 * only returns the Sanctuary while it is on the battlefield — dying alongside the creature (a
 * sweeper) must not pull it back out of the graveyard.
 */
val SquirrelSanctuary = card("Squirrel Sanctuary") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 green Squirrel creature token.\n" +
        "Whenever a nontoken creature you control dies, you may pay {1}. If you do, return this " +
        "enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            imageUri = "https://cards.scryfall.io/normal/front/9/7/977ddd05-1aae-46fc-95ce-866710d1c5c6.jpg?1783926589"
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.BATTLEFIELD),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed6ab503-5e9a-4339-a0e7-a909b2908f41.jpg?1783926824"
        ruling(
            "2021-06-18",
            "The last ability functions only while Squirrel Sanctuary is on the battlefield. " +
                "It won't return itself from your graveyard to your hand."
        )
    }
}
