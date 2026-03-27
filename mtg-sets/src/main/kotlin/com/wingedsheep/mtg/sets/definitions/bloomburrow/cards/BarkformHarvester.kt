package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Barkform Harvester
 * {3}
 * Artifact Creature — Shapeshifter
 * 2/3
 *
 * Changeling (This card is every creature type.)
 * Reach
 * {2}: Put target card from your graveyard on the bottom of your library.
 */
val BarkformHarvester = card("Barkform Harvester") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Shapeshifter"
    power = 2
    toughness = 3
    oracleText = "Changeling (This card is every creature type.)\nReach\n{2}: Put target card from your graveyard on the bottom of your library."

    keywords(Keyword.CHANGELING, Keyword.REACH)

    // {2}: Put target card from your graveyard on the bottom of your library
    activatedAbility {
        cost = Costs.Mana("{2}")
        val cardInGraveyard = target(
            "card in your graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        )
        effect = MoveToZoneEffect(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Zezhou Chen"
        flavorText = "The farmers hide when it runs out of potatoes to harvest."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f77049a6-0f22-415b-bc89-20bcb32accf6.jpg?1721427262"
    }
}
