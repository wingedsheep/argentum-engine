package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Divine Smite
 * {1}{W}
 * Instant
 * Target creature or planeswalker an opponent controls phases out. If that permanent is black,
 * exile it instead.
 *
 * The colour is checked as the spell resolves, against projected state.
 */
val DivineSmite = card("Divine Smite") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature or planeswalker an opponent controls phases out. If that permanent is black, " +
        "exile it instead. (If it phases out, treat it and anything attached to it as though they don't exist " +
        "until its controller's next turn.)"

    spell {
        val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.BLACK), t),
            then = Effects.Exile(t),
            otherwise = Effects.PhaseOut(t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Bryan Sola"
        flavorText = "\"When darkness closes in around us, my blade will light our path.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8d0852-4df3-4b29-830d-c6975265ef53.jpg?1783926534"
    }
}
