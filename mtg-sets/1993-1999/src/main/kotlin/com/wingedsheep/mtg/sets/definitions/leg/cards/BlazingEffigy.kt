package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blazing Effigy — Legends #134
 * {1}{R} · Creature — Elemental · 0/3
 *
 * When this creature dies, it deals X damage to target creature, where X is 3 plus the amount of
 * damage dealt to this creature this turn by other sources named Blazing Effigy.
 *
 * The damage count is last-known information captured as the Effigy dies: each source that
 * damaged it is recorded with its name and amount at damage time, so an Effigy whose own trigger
 * killed this one still counts after it has left the battlefield.
 */
val BlazingEffigy = card("Blazing Effigy") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 3
    oracleText = "When this creature dies, it deals X damage to target creature, where X is 3 plus the " +
        "amount of damage dealt to this creature this turn by other sources named Blazing Effigy."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.Creature)
        val effigyDamage = DynamicAmounts.lastKnownDamageDealtToSource(
            sourceFilter = GameObjectFilter.Any.named("Blazing Effigy"),
            excludeSelf = true
        )
        effect = Effects.DealDamage(3 + effigyDamage, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/921011ff-1696-4575-9198-abe993a0ee7a.jpg?1783948060"
    }
}
