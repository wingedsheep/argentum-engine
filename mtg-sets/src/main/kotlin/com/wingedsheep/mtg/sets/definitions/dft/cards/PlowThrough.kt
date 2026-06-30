package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Plow Through
 * {G}
 * Sorcery
 *
 * Choose one —
 * • Target creature you control fights target creature an opponent controls.
 * • Destroy target Vehicle.
 *
 * A true "Choose one" modal spell ([ModalEffect.chooseOne], counts as modal). Mode 1 is a
 * standard two-target fight; mode 2 destroys a Vehicle (an artifact with the Vehicle subtype).
 */
val PlowThrough = card("Plow Through") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target creature you control fights target creature an opponent controls. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "• Destroy target Vehicle."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: fight
            Mode(
                effect = Effects.Fight(EffectTarget.ContextTarget(0), EffectTarget.ContextTarget(1)),
                targetRequirements = listOf(Targets.CreatureYouControl, Targets.CreatureOpponentControls),
                description = "Target creature you control fights target creature an opponent controls",
            ),
            // Mode 2: destroy target Vehicle
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(
                    TargetPermanent(
                        filter = TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE)),
                    ),
                ),
                description = "Destroy target Vehicle",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Brian Valeza"
        flavorText = "Like most goblins, Rocketeers value nothing more than a good old-fashioned near-death experience."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a311d4b3-ab2a-43c2-8480-6c5daac41178.jpg?1782687824"
    }
}
