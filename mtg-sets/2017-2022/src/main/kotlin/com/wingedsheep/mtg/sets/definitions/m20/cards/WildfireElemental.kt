package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wildfire Elemental
 * {2}{R}{R}
 * Creature — Elemental
 * 3/3
 *
 * Whenever an opponent is dealt noncombat damage, creatures you control get +1/+0 until end of turn.
 *
 * The trigger is Chandra's Spitfire's any-source observer shape; the payoff is the Banners Raised
 * group pump, which only touches creatures you control as the ability resolves.
 */
val WildfireElemental = card("Wildfire Elemental") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "Whenever an opponent is dealt noncombat damage, creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
        description = "Whenever an opponent is dealt noncombat damage, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "297"
        artist = "Svetlin Velinov"
        flavorText = "\"Fire is always dancing, leaping and whirling, seeking more fuel. It never rests, so why should I?\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272e317c-55c4-43b2-91aa-3e0009cfd7d5.jpg?1783932917"

        ruling(
            "2019-07-12",
            "Combat damage is the damage that's dealt automatically by attacking and blocking creatures. " +
                "Any other damage is noncombat damage, even if it's dealt during a combat phase by an " +
                "attacking or blocking creature."
        )
        ruling(
            "2019-07-12",
            "The last ability of Wildfire Elemental triggers once for each event in which an opponent is " +
                "dealt noncombat damage, regardless of how much damage that player is dealt."
        )
        ruling(
            "2019-07-12",
            "Wildfire Elemental's triggered ability affects only creatures you control at the time it " +
                "resolves. Creatures you begin to control later in the turn won't get +1/+0."
        )
        ruling(
            "2019-07-12",
            "In a multiplayer game, if a source deals damage to multiple opponents at the same time, the " +
                "last ability of Wildfire Elemental will trigger that many times."
        )
    }
}
