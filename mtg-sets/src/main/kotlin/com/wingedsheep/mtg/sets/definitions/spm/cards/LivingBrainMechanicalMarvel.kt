package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Living Brain, Mechanical Marvel
 * {4}
 * Legendary Artifact Creature — Robot Villain
 * 3/3
 *
 * At the beginning of combat on your turn, target non-Equipment artifact you control
 * becomes an artifact creature with base power and toughness 3/3 until end of turn. Untap it.
 */
val LivingBrainMechanicalMarvel = card("Living Brain, Mechanical Marvel") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Robot Villain"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, target non-Equipment artifact you control becomes an artifact creature with base power and toughness 3/3 until end of turn. Untap it."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val artifact = target(
            "target non-Equipment artifact you control",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.Artifact.notSubtype(Subtype.EQUIPMENT).youControl())
            )
        )
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = artifact,
                power = 3,
                toughness = 3,
                duration = Duration.EndOfTurn
            ),
            Effects.Untap(artifact)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Nathaniel Himawan"
        flavorText = "\"This was cutting edge when I was a kid!\"\n—Spider-Man, Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26833b64-2e6d-4977-9a6e-6fe73c54d671.jpg?1757378042"
    }
}
