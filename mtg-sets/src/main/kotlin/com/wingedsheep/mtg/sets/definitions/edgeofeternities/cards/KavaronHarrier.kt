package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Kavaron Harrier
 * {R}
 * Artifact Creature — Robot Soldier
 * Whenever this creature attacks, you may pay {2}. If you do, create a 2/2 colorless Robot artifact creature token that's tapped and attacking. Sacrifice that token at end of combat.
 */
val KavaronHarrier = card("Kavaron Harrier") {
    manaCost = "{R}"
    typeLine = "Artifact Creature — Robot Soldier"
    oracleText = "Whenever this creature attacks, you may pay {2}. If you do, create a 2/2 colorless Robot artifact creature token that's tapped and attacking. Sacrifice that token at end of combat."
    power = 2
    toughness = 1

    // Triggered ability: Whenever this creature attacks, you may pay {2} to create a 2/2 Robot token
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}"),
            effect = CreateTokenEffect(
                power = 2,
                toughness = 2,
                colors = setOf(), // colorless
                creatureTypes = setOf("Robot"),
                imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130",
                tapped = true,
                attacking = true,
                artifactToken = true,
                exileAtStep = Step.END_COMBAT
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Hardy Fowler"
        flavorText = "Once the security for a moxite mine, now a ghost in a hollowed-out cavern."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1da2a740-e4ff-4661-ba57-39b15c58e26e.jpg?1752947115"
    }
}
