package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abrade
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • Abrade deals 3 damage to target creature.
 * • Destroy target artifact.
 */
val Abrade = card("Abrade") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Abrade deals 3 damage to target creature.\n• Destroy target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.DealDamage(3, EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Abrade deals 3 damage to target creature"
            ),
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Artifact,
                "Destroy target artifact"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1466ae6-9dbc-4ead-844b-9b1ba7274baf.jpg?1721429127"
    }
}
