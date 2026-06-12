package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bovine Intervention
 * {1}{W}
 * Instant
 *
 * Destroy target artifact or creature. Its controller creates a 2/2 white Ox creature token.
 *
 * "Its controller" is the controller of the destroyed permanent, resolved via
 * [EffectTarget.TargetController] (last-known information after the destroy).
 */
val BovineIntervention = card("Bovine Intervention") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or creature. Its controller creates a 2/2 white Ox creature token."

    spell {
        val permanent = target("permanent", Targets.CreatureOrArtifact)
        effect = Effects.Composite(
            listOf(
                Effects.Destroy(permanent),
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Ox"),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/c/e/cee3ecef-4566-4164-af39-89cb0bbbffeb.jpg?1712316060"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Julia Metzger"
        flavorText = "No one could prove the ox did it, but no one tried to harness it again either."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26c36742-456f-4618-99bc-793ef20b31b0.jpg?1712355244"
    }
}
