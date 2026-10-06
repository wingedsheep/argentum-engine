package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ob Nixilis's Cruelty — War of the Spark #101 (canonical printing)
 * {2}{B}
 * Instant
 * Target creature gets -5/-5 until end of turn. If that creature would die this turn, exile it instead.
 *
 * Same shape as Bleed Dry: the stat change, then the turn-long exile-on-death mark on the same target.
 */
val ObNixilissCruelty = card("Ob Nixilis's Cruelty") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -5/-5 until end of turn. If that creature would die this turn, exile it instead."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-5, -5, t) then Effects.MarkExileOnDeath(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Igor Kieryluk"
        flavorText = "Trapped on Ravnica with no affection for either side, Ob Nixilis sought gratification in random acts of torment."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94db72a8-57f7-4f17-9f09-cae90ef50304.jpg?1783933440"
    }
}
