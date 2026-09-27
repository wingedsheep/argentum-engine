package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rag Dealer — Champions of Kamigawa #138
 * {B} · Creature — Human Rogue · 1/1
 *
 * {2}{B}, {T}: Exile up to three target cards from a single graveyard.
 *
 * Same shape as Shred Memory / Griffnaut Tracker: "from a single graveyard" is the cross-target
 * `sameOwner` constraint, "up to three" is `count = 3, optional = true` (zero targets is a legal
 * activation that resolves doing nothing), and the payoff iterates the chosen targets.
 */
val RagDealer = card("Rag Dealer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "{2}{B}, {T}: Exile up to three target cards from a single graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        targets(TargetFilter.CardInGraveyard, count = 3, optional = true, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        )
        description = "Exile up to three target cards from a single graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Ralph Horsley"
        flavorText = "\"After General Takeno found the Oathkeeper amidst the bamboo marshes, more scavengers " +
            "braved the swamp's nezumi, oni, and kami in hopes of glory.\"\n—The History of Kamigawa"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18ef007a-fcf4-4293-933e-4f9f7f602002.jpg?1783944308"
    }
}
