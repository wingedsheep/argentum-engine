package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Seismic Shift
 * {3}{R}
 * Sorcery
 * Destroy target land. Up to two target creatures can't block this turn.
 */
val SeismicShift = card("Seismic Shift") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Up to two target creatures can't block this turn."

    spell {
        val land = target("target land", Targets.Land)
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = Effects.Destroy(land)
            .then(ForEachTargetEffect(listOf(CantBlockEffect(EffectTarget.ContextTarget(0)))))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "James Paick"
        flavorText = "Shiv is a restless land. It heaves ash, bleeds lava, and scabs over in black obsidian."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dad40a04-a026-4285-8c78-088a356652d1.jpg?1593861040"
    }
}
