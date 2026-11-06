package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trial of Knowledge
 * {3}{U}
 * Enchantment
 * When this enchantment enters, draw three cards, then discard a card.
 * When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
val TrialOfKnowledge = card("Trial of Knowledge") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw three cards, then discard a card.\n" +
        "When a Cartouche you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot(draw = 3)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cartouche").youControl()).enters()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89020c72-4cda-4634-8475-6d16a02236f1.jpg?1783936513"
        ruling(
            "2017-04-18",
            "Each Trial has an ability to return to your hand when a Cartouche enters the battlefield " +
                "under your control. The Trial is returned to its owner's hand only if it's on the " +
                "battlefield as the ability resolves.",
        )
    }
}
