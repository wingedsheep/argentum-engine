package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Uncontrolled Infestation
 * {1}{R}
 * Enchantment — Aura
 * Enchant nonbasic land
 * When enchanted land becomes tapped, destroy it.
 */
val UncontrolledInfestation = card("Uncontrolled Infestation") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonbasic land\nWhen enchanted land becomes tapped, destroy it."

    auraTarget = TargetPermanent(
        filter = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsLand,
                    CardPredicate.Not(CardPredicate.IsBasicLand)
                )
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.EnchantedPermanentBecomesTapped
        effect = MoveToZoneEffect(EffectTarget.EnchantedCreature, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Tony Szczudlo"
        flavorText = "\"The grounds of the Riptide Project are now populated only by slivers, broken beakers, and the lonely screeching of gulls.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9ead6c3-a4e9-43e0-ae2a-6eb73033bc49.jpg?1562535298"
    }
}
