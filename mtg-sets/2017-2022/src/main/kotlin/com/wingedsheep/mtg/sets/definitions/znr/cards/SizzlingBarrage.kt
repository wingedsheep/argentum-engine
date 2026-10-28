package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sizzling Barrage
 * {1}{R}
 * Instant
 * Sizzling Barrage deals 4 damage to target creature that blocked this turn.
 *
 * "Blocked this turn" is the turn-history [GameObjectFilter.blockedThisTurn] marker stamped at
 * blocker declaration, so a creature that blocked earlier in the turn and is no longer blocking
 * (combat over, or removed from combat) is still a legal target.
 */
val SizzlingBarrage = card("Sizzling Barrage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Sizzling Barrage deals 4 damage to target creature that blocked this turn."

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature.blockedThisTurn()))
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Johann Bodin"
        flavorText = "\"A thousand sparks can burn through your defenses just as quickly as one massive blaze.\"\n—Attikar, Akoum elementalist"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bf5843a-a01f-4d1c-b8fe-c586d5160f6e.jpg?1783929350"
        ruling("2020-09-25", "Sizzling Barrage can target a creature that's currently blocking or one that blocked earlier in the turn and is no longer blocking.")
        ruling("2020-09-25", "Destroying a blocking creature doesn't cause the creature it was blocking to become unblocked.")
    }
}
