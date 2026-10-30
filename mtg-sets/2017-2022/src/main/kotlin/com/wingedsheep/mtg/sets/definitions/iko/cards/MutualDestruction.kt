package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mutual Destruction — Ikoria: Lair of Behemoths #96
 * {B} · Sorcery
 *
 * This spell has flash as long as you control a permanent with flash.
 * As an additional cost to cast this spell, sacrifice a creature.
 * Destroy target creature.
 *
 * The flash clause is the card-level [conditionalFlash], gated on
 * [Conditions.YouControl] a permanent with the Flash keyword (read from projected state).
 */
val MutualDestruction = card("Mutual Destruction") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell has flash as long as you control a permanent with flash.\n" +
        "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Destroy target creature."

    conditionalFlash = Conditions.YouControl(GameObjectFilter.Permanent.withKeyword(Keyword.FLASH))

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "PINDURSKI"
        flavorText = "Hunter and hunted both died thinking they had outwitted the other."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85ac0b25-80bf-4871-a6f6-5cf4d5b9496e.jpg"
    }
}
