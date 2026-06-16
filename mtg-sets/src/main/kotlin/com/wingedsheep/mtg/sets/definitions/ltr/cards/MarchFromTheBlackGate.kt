package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * March from the Black Gate
 * {1}{B}
 * Enchantment
 *
 * When this enchantment enters and whenever an Army you control attacks, amass Orcs 1.
 */
val MarchFromTheBlackGate = card("March from the Black Gate") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters and whenever an Army you control attacks, amass Orcs 1. " +
        "(Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't control an Army, " +
        "create a 0/0 black Orc Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Amass(1, "Orc")
    }

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(
            GameObjectFilter.Creature.withSubtype("Army").youControl()
        )
        effect = Effects.Amass(1, "Orc")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        flavorText = "Like a storm, they broke upon the line of the Men of Gondor."
        artist = "Victor Harmatiuk"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e57815d4-b21f-4ceb-a3f1-73cff5f0e612.jpg?1686968563"
    }
}
