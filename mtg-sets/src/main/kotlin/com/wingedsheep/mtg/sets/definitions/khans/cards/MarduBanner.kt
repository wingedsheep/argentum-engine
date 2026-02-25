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
 * Mardu Banner
 * {3}
 * Artifact
 * {T}: Add {R}, {W}, or {B}.
 * {R}{W}{B}, {T}, Sacrifice this artifact: Draw a card.
 */
val MarduBanner = card("Mardu Banner") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R}, {W}, or {B}.\n{R}{W}{B}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

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
        cost = Costs.Composite(Costs.Mana("{R}{W}{B}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Daniel Ljunggren"
        flavorText = "\"Speed to strike, fury to smash.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe10c56d-a8e1-495d-a03a-0b920b44182f.jpg?1562796606"
    }
}
