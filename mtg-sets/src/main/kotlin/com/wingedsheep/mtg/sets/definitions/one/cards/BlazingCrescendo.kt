package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Blazing Crescendo
 * {1}{R}
 * Instant
 * Target creature gets +3/+1 until end of turn.
 * Exile the top card of your library. Until the end of your next turn, you may play that card.
 *
 * The exile-and-may-play clause is "impulse draw" with the two-turn permission window
 * ([MayPlayExpiry.UntilEndOfNextTurn]).
 */
val BlazingCrescendo = card("Blazing Crescendo") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+1 until end of turn.\nExile the top card of your library. Until the end of your next turn, you may play that card."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.ModifyStats(3, 1, t),
            Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilEndOfNextTurn)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Tiffany Turrill"
        flavorText = "\"They call themselves the Quiet Furnace? Ironic.\"\n—Tyvar"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6bfc16a-2871-40a4-b279-636b80491a06.jpg?1783918035"
    }
}
