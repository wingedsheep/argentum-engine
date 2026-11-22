package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Render Silent
 * {W}{U}{U}
 * Instant
 * Counter target spell. Its controller can't cast spells this turn.
 */
val RenderSilent = card("Render Silent") {
    manaCost = "{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Counter target spell. Its controller can't cast spells this turn."

    spell {
        target(TargetFilter.SpellOnStack)
        // The ban first, the same order as Ionize: "its controller" is read while the spell is
        // still on the stack (the countered card would otherwise fall back to its owner), and both
        // steps happen in one resolution, so the order is imperceptible to players. The ban still
        // applies if the spell can't be countered.
        effect = Effects.CantCastSpells(EffectTarget.TargetController) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3f3d6e4-0abe-4042-a7f6-0395683e8582.jpg?1783940023"
    }
}
