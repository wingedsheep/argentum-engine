package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Nuisance Engine — Mirrodin #221
 * {3} · Artifact · Uncommon
 *
 * {2}, {T}: Create a 0/1 colorless Pest artifact creature token.
 *
 * A repeatable chump-blocker factory. The whole card is one activated ability —
 * [Costs.Composite] of [Costs.Mana] "{2}" and [Costs.Tap] with a single [CreateTokenEffect]:
 * 0 power, 1 toughness, no colors (`colors = emptySet()`, which is what makes the token
 * *colorless* rather than merely uncolored-looking), `creatureTypes = setOf("Pest")` and
 * `artifactToken = true` for the artifact card type. The token defaults to the name "Pest Token".
 *
 * The tap cost is the throttle — one Pest per turn, and the Engine has to be untapped, so it
 * can't be used the turn it enters unless it was already on the battlefield.
 *
 * No `imageUri`: Mirrodin printed no token cards and Scryfall has no colorless artifact Pest token
 * in any printing, so the token falls through to the generic `TokenArt.IMAGES["Pest"]` stand-in
 * (added with this card) inside the client's generated frame, which carries the correct 0/1 and the
 * artifact-creature type line.
 */
val NuisanceEngine = card("Nuisance Engine") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Create a 0/1 colorless Pest artifact creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = CreateTokenEffect(
            power = 0,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Pest"),
            artifactToken = true
        )
        description = "{2}, {T}: Create a 0/1 colorless Pest artifact creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Stephen Tappin"
        flavorText = "All Auriok children know the tale, \"Bolgri and the Long Day of Squashing.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/266df8c3-5872-4d83-90bc-8f6f854ac838.jpg?1783944509"
    }
}
