package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter


/**
 * Devout Harpist
 * {W}
 * Creature — Human
 * 1/1
 * {T}: Destroy target Aura attached to a creature.
 */
val DevoutHarpist = card("Devout Harpist") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    oracleText = "{T}: Destroy target Aura attached to a creature."
    power = 1
    toughness = 1
    activatedAbility {
        cost = Costs.Tap
        val aura = target(
            TargetFilter(
                GameObjectFilter.Enchantment.withSubtype(Subtype.AURA).copy(
                    statePredicates = listOf(StatePredicate.AttachedTo(GameObjectFilter.Creature)),
                ),
            ),
        )
        effect = Effects.Destroy(aura)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Rebecca Guay"
        flavorText = "The notes themselves are irrelevant. The music's effect, however, is worth ten armies."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985b5c60-8a5e-4473-ba43-583aef50f19e.jpg"
    }
}
