package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Draconic Intervention
 * {2}{R}{R}
 * Sorcery
 * As an additional cost to cast this spell, exile an instant or sorcery card from your graveyard.
 * Draconic Intervention deals X damage to each non-Dragon creature, where X is the exiled card's
 * mana value. If a creature dealt damage this way would die this turn, exile it instead.
 * Exile Draconic Intervention.
 *
 * Composition:
 *  - The additional cost is Corpse Explosion's `ExileCards(GRAVEYARD)`; X reads the exiled card
 *    through `EffectTarget.ExiledAsCost()`.
 *  - Per non-Dragon creature, the damage instruction records how much it actually dealt
 *    (`damageDealtVariable`), and only a creature that was really dealt damage gets the
 *    "exile instead of dying this turn" mark. A creature whose damage was prevented (or X = 0)
 *    is not marked, so it still dies normally to anything else this turn.
 *  - The marks are placed during this one resolution, before state-based actions see the
 *    lethal damage, so the replacement is in place when the creatures would die.
 *  - `selfExile()` is "Exile Draconic Intervention."
 */
val DraconicIntervention = card("Draconic Intervention") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, exile an instant or sorcery card from your graveyard.\n" +
        "Draconic Intervention deals X damage to each non-Dragon creature, where X is the exiled card's mana value. " +
        "If a creature dealt damage this way would die this turn, exile it instead.\n" +
        "Exile Draconic Intervention."

    additionalCost(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.InstantOrSorcery,
            fromZone = CostZone.GRAVEYARD
        )
    )

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.DRAGON)),
            Effects.Pipeline {
                val dealt = runStoringNumber {
                    Effects.DealDamage(
                        DynamicAmounts.manaValueOf(EffectTarget.ExiledAsCost()),
                        EffectTarget.IterationEntity,
                        damageDealtVariable = it
                    )
                }
                ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                    run(Effects.MarkExileOnDeath(EffectTarget.IterationEntity))
                }
            }
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/657de246-b9fc-47b1-b932-091e9500bb82.jpg?1783927356"
    }
}
