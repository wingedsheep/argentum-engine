package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gruul Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {R} or {G}.
 * {R}{G}, {T}, Sacrifice this artifact: Draw a card.
 */
val GruulCluestone = card("Gruul Cluestone") {
    manaCost = "{3}"
    colorIdentity = "RG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R} or {G}.\n" +
        "{R}{G}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{R}{G}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Raoul Vitale"
        flavorText = "Forgotten under rocks and vines, Gruul symbols mark the sites of primitive rituals where shamans once chanted to the wild."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc47d1fe-8ab2-42f6-bcab-4bc2084ceba7.jpg"
    }
}
