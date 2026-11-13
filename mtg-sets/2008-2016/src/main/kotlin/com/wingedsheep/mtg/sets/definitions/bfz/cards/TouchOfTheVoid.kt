package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Touch of the Void (BFZ #134)
 * {2}{R}
 * Sorcery
 * Devoid
 * Touch of the Void deals 3 damage to any target. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Serpentine Spike's shape over a single "any target": the damage records what it really dealt
 * (`damageDealtVariable`, after prevention; 0 for a target that became illegal), and only a
 * *creature* actually dealt damage gets the "exile instead of dying this turn" mark. The creature
 * gate matters because the mark covers any permanent that dies — a planeswalker dealt damage this
 * way must still go to its owner's graveyard. Players are never marked.
 */
val TouchOfTheVoid = card("Touch of the Void") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Touch of the Void deals 3 damage to any target. If a creature dealt damage this way would " +
        "die this turn, exile it instead."

    keywords(Keyword.DEVOID)

    spell {
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(3, t, damageDealtVariable = it)
            }
            ifThen(
                Conditions.All(
                    Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0),
                    Conditions.EntityMatches(t, GameObjectFilter.Creature),
                )
            ) {
                run(Effects.MarkExileOnDeath(t))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/006ead4a-dc57-4856-8e13-235ba55483e6.jpg?1783938196"
        ruling("2015-08-25", "A creature doesn't necessarily have to be dealt lethal damage by Touch of the Void to be exiled. After being dealt damage, if it would die for any reason that turn, it'll be exiled instead.")
    }
}
