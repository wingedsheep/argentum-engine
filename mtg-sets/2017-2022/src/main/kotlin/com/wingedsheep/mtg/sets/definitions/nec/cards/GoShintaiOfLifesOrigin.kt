package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val SHRINE_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/9/0/900340a5-7517-4ec4-a51e-762e15a29470.jpg?1783923721"

private fun shrineToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    creatureTypes = setOf("Shrine"),
    enchantmentToken = true,
    imageUri = SHRINE_TOKEN_IMAGE,
)

/**
 * Go-Shintai of Life's Origin — Neon Dynasty Commander #37 (canonical printing)
 * {3}{G} · Legendary Enchantment Creature — Shrine · 3/4
 *
 * {W}{U}{B}{R}{G}, {T}: Return target enchantment card from your graveyard to the battlefield.
 * Whenever Go-Shintai of Life's Origin or another nontoken Shrine you control enters, create a 1/1
 * colorless Shrine enchantment creature token.
 *
 * The "this or another nontoken Shrine" trigger is written as two disjoint subjects — `self` and
 * `another(nontoken Shrine you control)` — rather than one `a(nontoken Shrine)` filter, because the
 * printed "nontoken" only qualifies the *other* Shrines: a token copy of Go-Shintai still triggers
 * its own ability (ruling). Every entering object matches at most one of the two, so exactly one
 * token is created per qualifying event.
 */
val GoShintaiOfLifesOrigin = card("Go-Shintai of Life's Origin") {
    manaCost = "{3}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 3
    toughness = 4
    oracleText = "{W}{U}{B}{R}{G}, {T}: Return target enchantment card from your graveyard to the battlefield.\n" +
        "Whenever Go-Shintai of Life's Origin or another nontoken Shrine you control enters, create a 1/1 " +
        "colorless Shrine enchantment creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{U}{B}{R}{G}"), Costs.Tap)
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(enchantment)
        description = "{W}{U}{B}{R}{G}, {T}: Return target enchantment card from your graveyard to the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = shrineToken()
        description = "Whenever Go-Shintai of Life's Origin or another nontoken Shrine you control enters, " +
            "create a 1/1 colorless Shrine enchantment creature token."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Any.withSubtype("Shrine").nontoken().youControl()).enters()
        effect = shrineToken()
        description = "Whenever Go-Shintai of Life's Origin or another nontoken Shrine you control enters, " +
            "create a 1/1 colorless Shrine enchantment creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "37"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9476fe67-d2d3-4835-8ba6-2a17d18cc141.jpg?1783923985"
        ruling("2022-02-18", "If a token copy of Go-Shintai of Life's Origin enters the battlefield, it will cause its own triggered ability to trigger.")
    }
}
