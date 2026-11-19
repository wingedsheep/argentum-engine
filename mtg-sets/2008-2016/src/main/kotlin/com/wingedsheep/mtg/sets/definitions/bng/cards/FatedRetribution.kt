package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fated Retribution — Born of the Gods #11 (canonical / earliest real printing)
 * {4}{W}{W}{W} · Instant
 *
 * Destroy all creatures and planeswalkers. If it's your turn, scry 2.
 *
 * One simultaneous [Effects.DestroyAll] over creatures-or-planeswalkers (In Garruk's Wake's
 * filter, unrestricted), then [Effects.Scry] gated on [Conditions.IsYourTurn] — the same
 * "If it's your turn, …" tail as Day's Undoing, checked at resolution.
 */
val FatedRetribution = card("Fated Retribution") {
    manaCost = "{4}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy all creatures and planeswalkers. If it's your turn, scry 2. " +
        "(Look at the top two cards of your library, then put any number of them on the bottom " +
        "of your library and the rest on top in any order.)"

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.CreatureOrPlaneswalker) then
            Effects.If(Conditions.IsYourTurn, Effects.Scry(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8158b330-2868-4147-907e-4d86e44cfaad.jpg?1783939583"
    }
}
