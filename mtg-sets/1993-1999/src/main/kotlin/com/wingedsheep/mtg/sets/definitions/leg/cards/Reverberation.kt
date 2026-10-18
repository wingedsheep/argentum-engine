package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reverberation — Legends #74
 * {2}{U}{U} · Instant
 *
 * All damage that would be dealt this turn by target sorcery spell is dealt to that spell's
 * controller instead.
 *
 * A source-keyed shield with no protected recipient: every recipient is covered, and damage the
 * spell would deal to its own controller simply stays there.
 */
val Reverberation = card("Reverberation") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "All damage that would be dealt this turn by target sorcery spell is dealt to that " +
        "spell's controller instead."

    spell {
        val sorcery = target(TargetFilter.SorcerySpellOnStack)
        effect = Effects.RedirectNextDamage(
            protectedTargets = emptyList(),
            redirectTo = EffectTarget.TargetController,
            scope = RedirectScope.CONTINUOUS,
            fromSource = sorcery
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Justin Hampton"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3d1f470-058d-41b7-acaf-4f68431de9ed.jpg?1783948072"
    }
}
