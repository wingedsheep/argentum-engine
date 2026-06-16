package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * A-Orcish Bowmasters (Alchemy rebalance of Orcish Bowmasters)
 * {1}{B}
 * Creature — Orc Archer
 * 1/1
 *
 * Flash
 * Whenever an opponent draws a card except the first one they draw in each of their draw steps,
 * Orcish Bowmasters deals 1 damage to any target. Then amass Orcs 1.
 *
 * The rebalanced (Arena) version drops the printed card's enters-the-battlefield trigger — only
 * the opponent-draw trigger remains. Same {1}{B} 1/1 Orc Archer with Flash. See
 * [OrcishBowmasters] for the original printing.
 */
val AOrcishBowmasters = card("A-Orcish Bowmasters") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Archer"
    power = 1
    toughness = 1
    oracleText = "Flash\nWhenever an opponent draws a card except the first one they draw in each of their draw steps, Orcish Bowmasters deals 1 damage to any target. Then amass Orcs 1."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.OpponentDrawsExceptFirstEachDrawStep
        val t = target("target", AnyTarget())
        effect = Effects.Composite(
            Effects.DealDamage(1, t),
            Effects.Amass(1, "Orc")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "A-103"
        artist = "Maxim Kostin"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff7c57ab-f07d-4653-9451-42821cb431c0.jpg?1730837116"
        // Alchemy rebalance — Scryfall booster:false; keep it out of the LTR draft/sealed pool.
        inBooster = false
    }
}
