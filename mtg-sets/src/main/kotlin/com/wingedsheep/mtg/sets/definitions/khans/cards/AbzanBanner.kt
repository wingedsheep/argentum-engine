package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Abzan Banner
 * {3}
 * Artifact
 * {T}: Add {W}, {B}, or {G}.
 * {W}{B}{G}, {T}, Sacrifice Abzan Banner: Draw a card.
 */
val AbzanBanner = card("Abzan Banner") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W}, {B}, or {G}.\n{W}{B}{G}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{B}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Daniel Ljunggren"
        flavorText = "\"Stone to endure, roots to remember.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7855528a-ede9-49a9-8749-795a004fd927.jpg?1562788826"
    }
}
