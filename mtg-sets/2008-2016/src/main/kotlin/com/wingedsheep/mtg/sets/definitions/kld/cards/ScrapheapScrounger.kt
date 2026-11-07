package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scrapheap Scrounger
 * {2}
 * Artifact Creature — Construct
 * 3/2
 *
 * This creature can't block.
 * {1}{B}, Exile another creature card from your graveyard: Return this card from your graveyard to
 * the battlefield.
 *
 * The recursion is activated from the graveyard, and the exile cost excludes the Scrounger itself
 * ([Costs.ExileAnotherFromGraveyard]) — it can't pay for its own return. Unlike Reassembling
 * Skeleton it returns untapped. Colorless card, black color identity from the activation cost.
 */
val ScrapheapScrounger = card("Scrapheap Scrounger") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    oracleText = "This creature can't block.\n" +
        "{1}{B}, Exile another creature card from your graveyard: Return this card from your graveyard " +
        "to the battlefield."
    power = 3
    toughness = 2

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.ExileAnotherFromGraveyard(1, GameObjectFilter.Creature),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Jason A. Engle"
        flavorText = "Some machines refuse to accept their own obsolescence."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/216a8e4e-ea7b-41c7-a982-b5751025ff25.jpg?1783937150"
    }
}
