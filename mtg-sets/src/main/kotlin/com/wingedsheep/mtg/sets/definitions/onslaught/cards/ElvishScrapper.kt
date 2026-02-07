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
        collectorNumber = "258"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Metal angers the elves. Everything they build is alive.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/large/front/a/e/ae85fafb-114b-4fd8-ac4c-5ada57054705.jpg?1562936242"
    }
}
