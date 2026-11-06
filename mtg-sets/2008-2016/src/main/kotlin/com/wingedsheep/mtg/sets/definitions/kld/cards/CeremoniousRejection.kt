package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ceremonious Rejection
 * {U}
 * Instant
 * Counter target colorless spell.
 */
val CeremoniousRejection = card("Ceremonious Rejection") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target colorless spell."

    spell {
        target(TargetFilter(GameObjectFilter(cardPredicates = listOf(CardPredicate.IsColorless)), zone = Zone.STACK))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Chris Rahn"
        flavorText = "The judges at the Fair are held in such high esteem that a nod of recognition can inspire an inventor for a lifetime—and a wave of dismissal can end a career."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08c5ed8e-4804-4042-8a1d-ad24c6846816.jpg?1783937222"
    }
}
