package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ugin's Construct
 * {4}
 * Artifact Creature — Construct
 * 4/5
 * When this creature enters, sacrifice a permanent that's one or more colors.
 *
 * "One or more colors" is [CardPredicate.IsColored], read off projected colors at resolution, so
 * a controller with only colorless permanents (basic lands, face-down permanents, the Construct
 * itself) sacrifices nothing.
 */
val UginsConstruct = card("Ugin's Construct") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "When this creature enters, sacrifice a permanent that's one or more colors."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.SacrificeOwn(GameObjectFilter.Permanent.withCardPredicate(CardPredicate.IsColored))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Peter Mohrbacher"
        flavorText = "While trapping the Eldrazi on Zendikar, Ugin learned little from Sorin, but he gleaned the rudiments of lithomancy from Nahiri."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ee009a4-72d0-4828-b826-3ed7c7d87b53.jpg?1783938670"
        ruling("2014-11-24", "If you control only colorless permanents as the ability resolves (for example, basic lands and face-down permanents), you won't sacrifice anything.")
    }
}
