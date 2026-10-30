package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Academy Ruins — Time Spiral #269
 * Legendary Land
 *
 * {T}: Add {C}.
 * {1}{U}, {T}: Put target artifact card from your graveyard on top of your library.
 */
val AcademyRuins = card("Academy Ruins") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}{U}, {T}: Put target artifact card from your graveyard on top of your library."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOnTopOfLibrary(artifact)
        description = "Put target artifact card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "269"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "Its secrets once wrought the greatest artifice ever known. Now crabs loot the rubble to decorate their shells."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af09af65-0a3b-42df-b0fd-372e2158beac.jpg?1783943196"
    }
}
