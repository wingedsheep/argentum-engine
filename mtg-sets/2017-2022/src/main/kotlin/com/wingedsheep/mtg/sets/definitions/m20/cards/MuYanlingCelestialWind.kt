package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mu Yanling, Celestial Wind
 * {4}{U}{U}
 * Legendary Planeswalker — Yanling
 * Loyalty: 5
 *
 * +1: Until your next turn, up to one target creature gets -5/-0.
 * −3: Return up to two target creatures to their owners' hands.
 * −7: Creatures you control with flying get +5/+5 until end of turn.
 *
 * The −7 locks in the flyers you control as it resolves (per-entity pump, per the ruling).
 */
val MuYanlingCelestialWind = card("Mu Yanling, Celestial Wind") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Yanling"
    startingLoyalty = 5
    oracleText = "+1: Until your next turn, up to one target creature gets -5/-0.\n" +
        "−3: Return up to two target creatures to their owners' hands.\n" +
        "−7: Creatures you control with flying get +5/+5 until end of turn."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-5, 0, creature, Duration.UntilYourNextTurn)
        description = "Until your next turn, up to one target creature gets -5/-0."
    }

    loyaltyAbility(-3) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
        description = "Return up to two target creatures to their owners' hands."
    }

    loyaltyAbility(-7) {
        effect = Patterns.Group.modifyStatsForAll(
            5, 5,
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl())
        )
        description = "Creatures you control with flying get +5/+5 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "286"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84ae9595-8e76-4d8c-a08c-00434be75672.jpg?1783932921"
        ruling("2019-07-12", "Mu Yanling's last ability affects only creatures you control with flying at the time it resolves. Creatures you begin to control later in the turn (or creatures that gain flying later in the turn) won't get +5/+5.")
        ruling("2019-07-12", "In a multiplayer game, if you leave the game after Mu Yanling's first ability resolves but before your next turn begins, its effect lasts until your next turn would have begun. It neither expires immediately nor lasts indefinitely.")
    }
}
