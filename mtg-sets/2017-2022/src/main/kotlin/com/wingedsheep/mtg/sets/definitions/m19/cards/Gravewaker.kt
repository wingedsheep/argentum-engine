package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gravewaker — Core Set 2019 #293 (Liliana planeswalker deck)
 * {4}{B}{B} · Creature — Bird Spirit · Rare
 * 5/5
 *
 * Flying
 * {5}{B}{B}: Return target creature card from your graveyard to the battlefield tapped.
 *
 * Canonical earliest real printing: Core Set 2019 (M19). Reprinted in Core Set 2020 (M20).
 */
val Gravewaker = card("Gravewaker") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "{5}{B}{B}: Return target creature card from your graveyard to the battlefield tapped."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{5}{B}{B}")
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard, tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "293"
        artist = "Daniel Ljunggren"
        flavorText = "Once every seven years it flies on spectral wings over the moors of ancient battlefields."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee347d62-867e-40c2-bf4a-0ac2f627e24c.jpg?1783934483"
    }
}
