package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mondrak, Glory Dominus
 * {2}{W}{W}
 * Legendary Creature — Phyrexian Horror
 * 4/4
 *
 * If one or more tokens would be created under your control, twice that many of those tokens are
 * created instead.
 * {1}{W/P}{W/P}, Sacrifice two other artifacts and/or creatures: Put an indestructible counter on
 * Mondrak.
 *
 * Token doubling is Doubling Season's first clause ([MultiplyTokenCreation] with its default
 * "under your control" pattern). The activated ability mirrors its Dominus siblings (Zopandrel,
 * Drivnod): `SacrificeMultiple` over an artifact-or-creature filter with `notSourceItself()`, so
 * Mondrak can never be one of the two.
 */
val MondrakGloryDominus = card("Mondrak, Glory Dominus") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Phyrexian Horror"
    power = 4
    toughness = 4
    oracleText =
        "If one or more tokens would be created under your control, twice that many of those tokens are created instead.\n" +
        "{1}{W/P}{W/P}, Sacrifice two other artifacts and/or creatures: Put an indestructible counter on Mondrak. " +
        "({W/P} can be paid with either {W} or 2 life.)"

    replacementEffect(MultiplyTokenCreation())

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W/P}{W/P}"),
            Costs.SacrificeMultiple(
                2,
                (GameObjectFilter.Artifact or GameObjectFilter.Creature).notSourceItself()
            )
        )
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "{1}{W/P}{W/P}, Sacrifice two other artifacts and/or creatures: Put an indestructible counter on Mondrak."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "23"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8296a455-21d5-498e-9029-2bdf0da855a8.jpg?1783918077"
        ruling("2023-02-04", "Everything that is specified by the effect creating the original token or tokens will also be true about the additional token or tokens created by Mondrak's replacement effect. For example, if an effect tells you to create a token \"tapped and attacking,\" the additional tokens will also be tapped and attacking. Similarly, if an effect creates a token and puts counters on it (such as a Fractal token) the additional token will also get those counters.")
    }
}
