package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

/**
 * Elvish Scrapper
 * {G}
 * Creature — Elf
 * 1/1
 * {G}, {T}, Sacrifice Elvish Scrapper: Destroy target artifact.
 */
val ElvishScrapper = card("Elvish Scrapper") {
    manaCost = "{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap, Costs.SacrificeSelf)
        target = Targets.Artifact
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Greg Staples"
        flavorText = "\"Metal angers the elves. Everything they build is alive.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f8fd8f2-7c44-4149-83df-1f4ae57e51a4.jpg?1562926158"
    }
}
