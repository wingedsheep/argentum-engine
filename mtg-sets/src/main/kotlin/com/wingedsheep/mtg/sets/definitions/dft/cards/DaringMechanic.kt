package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Daring Mechanic — Aetherdrift #11
 * {2}{W} · Creature — Human Artificer · 3/3
 *
 * {3}{W}: Put a +1/+1 counter on target Mount or Vehicle.
 *
 * "Mount or Vehicle" is a permanent whose subtype is Mount or Vehicle, modeled as a
 * [GameObjectFilter] with an Or over the two subtype predicates (there is no preset for this
 * pairing, but both are plain subtypes).
 */
private val MountOrVehicle = GameObjectFilter(
    cardPredicates = listOf(
        CardPredicate.Or(
            listOf(
                CardPredicate.HasSubtype(Subtype("Mount")),
                CardPredicate.HasSubtype(Subtype.VEHICLE)
            )
        )
    )
)

val DaringMechanic = card("Daring Mechanic") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "{3}{W}: Put a +1/+1 counter on target Mount or Vehicle."

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val t = target("target Mount or Vehicle", TargetPermanent(filter = TargetFilter(MountOrVehicle)))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Elizabeth Peiró"
        flavorText = "\"Just a few more bolts—SHARK ON YOUR RIGHT—and we should be good to go.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3382552c-2740-409a-83a1-80b60627beb8.jpg?1782687954"
    }
}
