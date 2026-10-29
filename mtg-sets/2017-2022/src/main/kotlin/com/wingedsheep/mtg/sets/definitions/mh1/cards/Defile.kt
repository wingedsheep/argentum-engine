package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Defile — Modern Horizons #86
 * {B} · Instant
 *
 * Target creature gets -1/-1 until end of turn for each Swamp you control.
 *
 * The Irradiate shape: a resolution-time [Effects.ModifyStats] over the count of Swamps you
 * control (projected subtype, so nonbasic Swamps count), locked in as Defile resolves.
 */
val Defile = card("Defile") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-1 until end of turn for each Swamp you control."

    spell {
        val t = target(TargetFilter.Creature)
        val negSwamps = -DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype(Subtype.SWAMP)
        ).count()
        effect = Effects.ModifyStats(negSwamps, negSwamps, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Joe Slucher"
        flavorText = "The only shortcut in a bog is to the bottom."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bcb4398-edd1-41a7-a496-b12bce22ceb6.jpg?1783933130"
    }
}
