package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Psychic Drain — Ravnica: City of Guilds #220
 * {X}{U}{B} · Sorcery
 *
 * Target player mills X cards and you gain X life.
 *
 * Modelling notes:
 * - Both halves read [DynamicAmount.XValue], the X chosen on casting. The life gain is *not*
 *   scoped to how many cards were actually milled, so a library shorter than X still gains the
 *   full X (CR 701.17b mills what it can; the second sentence is independent of it).
 * - One target for the whole spell: if the player becomes an illegal target the spell doesn't
 *   resolve at all and no life is gained, which falls out of the engine's fizzle path.
 */
val PsychicDrain = card("Psychic Drain") {
    manaCost = "{X}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player mills X cards and you gain X life."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.xValue(), player) then
            Effects.GainLife(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Nick Percival"
        flavorText = "\"Gold can be reearned, goods restored. The moroii steal youth, more " +
            "precious than either, and once gone, it's gone forever.\"\n—Berta Suszat, civic healer"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d00fd2d8-76ce-4334-b772-a5dc64fc2989.jpg?1783943615"
    }
}
