package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hearthborn Battler
 * {2}{R}
 * Creature — Lizard Warlock
 * 2/3
 *
 * Haste
 * Whenever a player casts their second spell each turn,
 * this creature deals 2 damage to target opponent.
 */
val HearthbornBattler = card("Hearthborn Battler") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Lizard Warlock"
    oracleText = "Haste\n" +
        "Whenever a player casts their second spell each turn, " +
        "this creature deals 2 damage to target opponent."
    power = 2
    toughness = 3

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2)
        val opponent = target("opponent", Targets.Opponent)
        effect = Effects.DealDamage(2, opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bef1cf5c-9738-4062-8cb1-87a372d36687.jpg?1721426641"
    }
}
