package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Boros Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {R} or {W}.
 * {R}{W}, {T}, Sacrifice this artifact: Draw a card.
 */
val BorosCluestone = card("Boros Cluestone") {
    manaCost = "{3}"
    colorIdentity = "RW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R} or {W}.\n" +
        "{R}{W}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R}{W}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Raoul Vitale"
        flavorText = "\"In case of fire, treachery, citywide riot, political upheaval, or worldwide societal collapse, break glass.\"\n—Cluestone inscription"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87252577-3e7b-4ea2-b0ac-3ba3f0eaac40.jpg"
    }
}
