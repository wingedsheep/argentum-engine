package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bone Dragon
 * {3}{B}{B}
 * Creature — Dragon Skeleton
 * 5/4
 * Flying
 * {3}{B}{B}, Exile seven other cards from your graveyard: Return this card from your graveyard to
 * the battlefield tapped.
 */
val BoneDragon = card("Bone Dragon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon Skeleton"
    oracleText = "Flying\n" +
        "{3}{B}{B}, Exile seven other cards from your graveyard: Return this card from your graveyard " +
        "to the battlefield tapped."
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}{B}"), Costs.ExileAnotherFromGraveyard(7))
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Jason A. Engle"
        flavorText = "Its roar is the clatter of a thousand bones."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08a0de62-6e9f-4ee9-9d8d-ee6f6a115307.jpg?1783934576"
    }
}
