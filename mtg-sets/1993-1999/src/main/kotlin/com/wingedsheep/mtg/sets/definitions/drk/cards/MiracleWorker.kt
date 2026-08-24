package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Miracle Worker
 * {W}
 * Creature — Human Cleric
 * 1/1
 * {T}: Destroy target Aura attached to a creature you control.
 *
 * The target is the *Aura*, not the creature it enchants. The host restriction is narrower than
 * Savaen Elves' — "a creature you control" rather than any land — so it needs the filter-taking
 * [StatePredicate.AttachedTo] rather than the card-type-only `AttachedToCardType`.
 */
val MiracleWorker = card("Miracle Worker") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{T}: Destroy target Aura attached to a creature you control."

    activatedAbility {
        cost = Costs.Tap
        val aura = target(
            "target",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Enchantment.withSubtype("Aura").copy(
                        statePredicates = listOf(
                            StatePredicate.AttachedTo(GameObjectFilter.Creature.youControl())
                        )
                    )
                )
            )
        )
        effect = Effects.Destroy(aura)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Ron Spencer"
        flavorText = "\"Those blessed hands could bring surcease to even the most tainted soul.\"\n" +
            "—Sister Betje, *Miracles of the Saints*"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35d29bda-096c-44d4-b45e-c2c507f8efbe.jpg?1783947946"
    }
}
