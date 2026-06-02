package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rooftop Percher
 * {5}
 * Creature — Shapeshifter
 * 3/3
 *
 * Changeling (This card is every creature type.)
 * Flying
 * When this creature enters, exile up to two target cards from graveyards. You gain 3 life.
 */
val RooftopPercher = card("Rooftop Percher") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Flying\n" +
        "When this creature enters, exile up to two target cards from graveyards. You gain 3 life."

    keywords(Keyword.CHANGELING, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "cards from graveyards",
            TargetObject(count = 2, optional = true, filter = TargetFilter.CardInGraveyard)
        )
        effect = ForEachTargetEffect(
            listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE))
        ).then(Effects.GainLife(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d89595c-1542-41fe-8d23-997522922698.jpg?1767732447"
    }
}
