package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Prosperous Bandit
 * {2}{R}
 * Creature — Raccoon Rogue
 * 2/2
 *
 * Offspring {1} (You may pay an additional {1} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 * First strike
 * Whenever this creature deals combat damage to a player, create that many tapped
 * Treasure tokens.
 */
val ProsperousBandit = card("Prosperous Bandit") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Raccoon Rogue"
    power = 2
    toughness = 2
    oracleText = "Offspring {1} (You may pay an additional {1} as you cast this spell. If you do, " +
        "when this creature enters, create a 1/1 token copy of it.)\n" +
        "First strike\n" +
        "Whenever this creature deals combat damage to a player, create that many tapped Treasure tokens."

    keywords(Keyword.FIRST_STRIKE)

    // Offspring: modeled as kicker-like additional cost
    keywordAbility(KeywordAbility.offspring("{1}"))

    // Offspring ETB: when this enters, if offspring was paid, create a 1/1 token copy
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // Whenever this creature deals combat damage to a player, create that many
    // tapped Treasure tokens.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = RepeatDynamicTimesEffect(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            body = Effects.CreateTreasure(tapped = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Yohann Schepacz"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b3a7194-25e8-4639-a490-96e9cfce0f84.jpg?1721428257"
    }
}
