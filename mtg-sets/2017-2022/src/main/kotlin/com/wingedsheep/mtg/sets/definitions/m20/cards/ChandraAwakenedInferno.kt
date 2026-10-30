package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, Awakened Inferno
 * {4}{R}{R}
 * Legendary Planeswalker — Chandra
 *
 *  - **+2** gives each opponent an emblem of their own: `ForEachPlayer(EachOpponent, …)` rebinds the
 *    controller per opponent, so "your upkeep" and "deals 1 damage to you" both mean that opponent.
 *    `emblem = true` makes the colorless emblem the damage source (not the red planeswalker), and the
 *    emblems outlive Chandra and her controller (rulings).
 *  - **−X** is Crush the Weak's shape: the "exile instead" mark goes on only if damage was actually
 *    dealt, so prevented damage leaves the permanent dying normally (ruling).
 */
val ChandraAwakenedInferno = card("Chandra, Awakened Inferno") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 6
    cantBeCountered = true
    oracleText = "This spell can't be countered.\n" +
        "+2: Each opponent gets an emblem with \"At the beginning of your upkeep, this emblem deals " +
        "1 damage to you.\"\n" +
        "−3: Chandra deals 3 damage to each non-Elemental creature.\n" +
        "−X: Chandra deals X damage to target creature or planeswalker. If a permanent dealt damage " +
        "this way would die this turn, exile it instead."

    loyaltyAbility(+2) {
        val emblemText = "At the beginning of your upkeep, this emblem deals 1 damage to you."
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.CreateGlobalTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.you.beginningOf(Step.UPKEEP)
                    effect = Effects.DealDamage(1, EffectTarget.Controller)
                    description = emblemText
                },
                descriptionOverride = emblemText,
                emblem = true
            )
        )
        description = "Each opponent gets an emblem with \"$emblemText\""
    }

    loyaltyAbility(-3) {
        effect = Patterns.Group.dealDamageToAll(
            3,
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype("Elemental")))
        )
    }

    loyaltyAbilityX {
        val permanent = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(DynamicAmounts.xValue(), permanent, damageDealtVariable = it)
            }
            ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                run(Effects.MarkExileOnDeath(permanent))
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "127"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d2a680-4f3b-4bfa-b77b-d2dfaced9f23.jpg?1783932983"
        ruling("2019-07-12", "A spell or ability that counters spells can still target Chandra. When that spell or ability resolves, Chandra won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2019-07-12", "Emblems aren't permanents and can't be exiled or destroyed. Once you have an emblem, there's no way to remove it.")
        ruling("2019-07-12", "The ability of Chandra's emblem triggers at the beginning of the upkeep of the player who gets the emblem, not the upkeep of Chandra's controller.")
        ruling("2019-07-12", "A player may have more than one of Chandra's emblems. Each one's ability triggers separately.")
        ruling("2019-07-12", "The replacement effect of Chandra's last ability will exile the target creature or planeswalker if it would die this turn for any reason, not just due to lethal damage.")
        ruling("2019-07-12", "If Chandra's last ability doesn't deal damage (most likely because the damage has been prevented in some way), the target won't be exiled if it would die.")
        ruling("2019-07-12", "If you leave a multiplayer game, your opponents keep any emblems Chandra gave them. Each opponent owns the emblem Chandra gives them. In multiplayer, your opponents continue to burn after you and Chandra leave the game.")
    }
}
