package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Parcel Myr — Modern Horizons 2 #54
 * {1}{U} · Artifact Creature — Clue Myr · 2 / 1
 *
 * {2}, Sacrifice this creature: Draw a card.
 *
 * A Myr that is also a Clue — the subtype is on the printed type line, and Scryfall's standing
 * ruling is that "a Clue" means any Clue artifact, not only a Clue token, so the set's
 * "sacrifice a Clue" payoffs read it straight off the type line with no per-card wiring.
 *
 * The ability is the Clue's own printed sacrifice-to-draw, spelled out here rather than inherited:
 * a Clue token's ability belongs to the token, and this is a real card. [Costs.SacrificeSelf] is
 * the "Sacrifice this creature" atom, and because it is a cost the body is gone before the draw
 * resolves — so the card can be sacrificed in response to removal and still cash in.
 */
val ParcelMyr = card("Parcel Myr") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Clue Myr"
    power = 2
    toughness = 1
    oracleText = "{2}, Sacrifice this creature: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "{2}, Sacrifice this creature: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Wisnu Tan"
        flavorText = "\"Crack it open. Maybe it knows something we can use.\"\n—Kara Vrist, Mirran resistance"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f288ee5-4bf9-476a-a89f-b6b8fa7e87dc.jpg?1783926875"
    }
}
