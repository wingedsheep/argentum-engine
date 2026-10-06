package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Necrotic Wound
 * {B}
 * Instant
 * Undergrowth — Target creature gets -X/-X until end of turn, where X is the number of creature
 * cards in your graveyard. If that creature would die this turn, exile it instead.
 *
 * X is locked in at resolution ([Effects.ModifyStats] with a [DynamicAmount]); the exile-on-death
 * mark is applied first so it is already in place when state-based actions see the -X/-X.
 */
val NecroticWound = card("Necrotic Wound") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Undergrowth — Target creature gets -X/-X until end of turn, where X is the number of creature cards in your graveyard. If that creature would die this turn, exile it instead."

    spell {
        val t = target(TargetFilter.Creature)
        val negX = -DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.MarkExileOnDeath(t) then Effects.ModifyStats(negX, negX, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Randy Vargas"
        flavorText = "The assassins of the Ochran distill toxins from the remains of their previous victims."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ab636af-5b30-4138-8e68-81f567f10417.jpg?1783934171"
    }
}
