package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Serpentine Spike (BFZ #133)
 * {5}{R}{R}
 * Sorcery
 * Devoid
 * Serpentine Spike deals 2 damage to target creature, 3 damage to another target creature, and 4
 * damage to a third target creature. If a creature dealt damage this way would die this turn, exile
 * it instead.
 *
 * Cone of Flame's targeting over creatures: the second and third slots are [TargetOther], so all
 * three targets are different creatures. Each damage instruction records what it really dealt
 * (`damageDealtVariable`, after prevention; 0 for a target that became illegal), and only a
 * creature actually dealt damage gets the "exile instead of dying this turn" mark — Crush the
 * Weak's shape, once per target. The marks land during resolution, before state-based actions see
 * the lethal damage.
 */
val SerpentineSpike = card("Serpentine Spike") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Serpentine Spike deals 2 damage to target creature, 3 damage to another target creature, " +
        "and 4 damage to a third target creature. If a creature dealt damage this way would die " +
        "this turn, exile it instead."

    keywords(Keyword.DEVOID)

    spell {
        val first = target(TargetFilter.Creature)
        val second = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        val third = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.Pipeline {
            for ((amount, creature) in listOf(2 to first, 3 to second, 4 to third)) {
                val dealt = runStoringNumber {
                    Effects.DealDamage(amount, creature, damageDealtVariable = it)
                }
                ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                    run(Effects.MarkExileOnDeath(creature))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53222f33-6d18-4008-a38d-6f7995691260.jpg?1783938196"
        ruling("2015-08-25", "Each target must be a different creature. You can't cast Serpentine Spike without three different creatures available.")
        ruling("2015-08-25", "If one or two of those targets become illegal by the time Serpentine Spike resolves, you can't change how much damage will be dealt to the remaining legal targets.")
        ruling("2015-08-25", "A creature doesn't necessarily have to be dealt lethal damage by Serpentine Spike to be exiled. After being dealt damage, if it would die for any reason that turn, it'll be exiled instead.")
    }
}
