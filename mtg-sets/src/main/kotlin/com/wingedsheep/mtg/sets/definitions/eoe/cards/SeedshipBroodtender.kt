package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Seedship Broodtender
 * {B}{G}
 * Creature — Insect Citizen
 * When this creature enters, mill three cards. (Put the top three cards of your library into your graveyard.)
 * {3}{B}{G}, Sacrifice this creature: Return target creature or Spacecraft card from your graveyard to the battlefield. Activate only as a sorcery.
 */
val SeedshipBroodtender = card("Seedship Broodtender") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Insect Citizen"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill three cards. (Put the top three cards of your library into your graveyard.)\n{3}{B}{G}, Sacrifice this creature: Return target creature or Spacecraft card from your graveyard to the battlefield. Activate only as a sorcery."

    // ETB ability: mill three cards
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.mill(3)
    }

    // Activated ability: reanimation, sorcery speed only
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}{G}"),
            Costs.SacrificeSelf
        )
        timing = TimingRule.SorcerySpeed
        
        val graveyardTarget = target("creature or Spacecraft card from your graveyard", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Companion.Creature.ownedByYou().or(GameObjectFilter.Companion.Permanent.withSubtype("Spacecraft").ownedByYou()),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = Effects.PutOntoBattlefield(graveyardTarget)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Eric Wilkerson"
        flavorText = "\"Inspiration blossoms from fertile rot.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1abc176f-2ccf-4371-b4b5-030dd99ff7fc.jpg?1752947486"
    }
}
