package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trial of Zeal
 * {2}{R}
 * Enchantment
 * When this enchantment enters, it deals 3 damage to any target.
 * When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
val TrialOfZeal = card("Trial of Zeal") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, it deals 3 damage to any target.\n" +
        "When a Cartouche you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(3, any)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cartouche").youControl()).enters()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/543539ce-af50-4bbe-82aa-f35fd2c2a256.jpg?1783936481"
        ruling(
            "2017-04-18",
            "Each Trial has an ability to return to your hand when a Cartouche enters the battlefield " +
                "under your control. The Trial is returned to its owner's hand only if it's on the " +
                "battlefield as the ability resolves.",
        )
    }
}
