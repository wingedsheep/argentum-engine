package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Resculpt — Strixhaven: School of Mages #51 (canonical printing)
 * {1}{U} · Instant
 *
 * Exile target artifact or creature. Its controller creates a 4/4 blue and red Elemental creature
 * token.
 *
 * Reduce to Memory's shape over an artifact-or-creature target: the exile, then the set's 4/4
 * blue and red Elemental for the exiled permanent's controller ([EffectTarget.TargetController]).
 */
val Resculpt = card("Resculpt") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile target artifact or creature. Its controller creates a 4/4 blue and red " +
        "Elemental creature token."

    spell {
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.Exile(t) then Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental"),
            controller = EffectTarget.TargetController,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Torstein Nordstrand"
        flavorText = "Critics were struck first by the elemental's surprising grace, and then by its fists."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bfb1fcb-a411-4c1c-b8fc-496242ae3a9b.jpg?1783927375"
    }
}
