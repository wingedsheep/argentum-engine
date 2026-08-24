package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grave Robbers
 * {1}{B}{B}
 * Creature — Human Rogue
 * 1/1
 * {B}, {T}: Exile target artifact card from a graveyard. You gain 2 life.
 *
 * "a graveyard" is any player's, so the target filter carries no owner predicate. The life gain
 * is part of the same resolution, so it happens even though the exile has already left the card
 * out of reach.
 */
val GraveRobbers = card("Grave Robbers") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "{B}, {T}: Exile target artifact card from a graveyard. You gain 2 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val artifact = target(
            "target artifact card from a graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Artifact, zone = Zone.GRAVEYARD))
        )
        effect = Effects.Composite(
            Effects.Exile(artifact, fromZone = Zone.GRAVEYARD),
            Effects.GainLife(2)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Quinton Hoover"
        flavorText = "\"If you don't have your health, you don't have anything.\"\n—Proverb"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a131605a-f646-4745-a1e4-48d155a3d94f.jpg?1783947938"

        ruling("2004-10-04", "This exiles the artifact on resolution, but you choose it as a target when activating the ability.")
        ruling("2004-10-04", "If the target is not there on resolution, you do not gain the life.")
    }
}
