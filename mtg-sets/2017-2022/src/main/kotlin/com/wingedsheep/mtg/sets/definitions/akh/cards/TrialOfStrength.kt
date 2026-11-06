package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trial of Strength
 * {2}{G}
 * Enchantment
 * When this enchantment enters, create a 4/2 green Beast creature token.
 * When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
val TrialOfStrength = card("Trial of Strength") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 4/2 green Beast creature token.\n" +
        "When a Cartouche you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cartouche").youControl()).enters()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Kieran Yanner"
        flavorText = "Instincts to guide. Strength to overcome."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acadb754-4a3b-4633-b784-252c0e1ab650.jpg?1783936465"
    }
}
