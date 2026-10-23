package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Corpse Cobble — Innistrad: Midnight Hunt #214
 * {U}{B} · Instant
 *
 * As an additional cost to cast this spell, sacrifice any number of creatures.
 * Create an X/X blue and black Zombie creature token with menace, where X is the total power of the
 * sacrificed creatures.
 * Flashback {3}{U}{B}
 *
 * The variable sacrifice cost (minimum zero) snapshots each creature as it last existed on the
 * battlefield, so X reads their power after pumps and anthems. Sacrificing nothing makes a 0/0 that
 * dies to state-based actions; flashback still charges the additional cost.
 */
val CorpseCobble = card("Corpse Cobble") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice any number of creatures.\n" +
        "Create an X/X blue and black Zombie creature token with menace, where X is the total power of " +
        "the sacrificed creatures.\n" +
        "Flashback {3}{U}{B} (You may cast this card from your graveyard for its flashback cost and any " +
        "additional costs. Then exile it.)"

    additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Creature))

    spell {
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.totalPowerSacrificedThisWay(),
            dynamicToughness = DynamicAmounts.totalPowerSacrificedThisWay(),
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/e/b/eb5fb489-6e46-4359-9c3c-ef1702526e5a.jpg?1783925219",
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{U}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Ravenna Tran"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe4f48d-179a-47d7-aed6-9751dd2f7d8c.jpg?1783925566"
        ruling("2021-09-24", "If you cast Corpse Cobble using flashback, you still pay its additional cost of sacrificing any number of creatures.")
        ruling("2021-09-24", "You may choose to sacrifice no creatures to pay the additional cost. If you do, you'll create a 0/0 Zombie token. Unless something else is raising its toughness, the token will subsequently die, but any \"enters the battlefield\" or \"dies\" triggered abilities will still trigger.")
    }
}
