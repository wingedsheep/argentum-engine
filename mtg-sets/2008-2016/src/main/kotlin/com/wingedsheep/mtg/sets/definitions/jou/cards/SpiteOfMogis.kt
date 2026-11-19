package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spite of Mogis
 * {R}
 * Sorcery
 * Spite of Mogis deals damage to target creature equal to the number of instant and sorcery cards
 * in your graveyard. Scry 1.
 *
 * The spell is still on the stack while the count is taken, so it never counts itself.
 */
val SpiteOfMogis = card("Spite of Mogis") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Spite of Mogis deals damage to target creature equal to the number of instant and sorcery " +
        "cards in your graveyard. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery), t) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5676d255-707e-4621-a0df-3305f999f124.jpg?1783939418"
        ruling("2014-04-26", "Spite of Mogis isn't counted among the number of instant and sorcery cards in your graveyard. It isn't put there until after it deals damage and you scry 1.")
    }
}
