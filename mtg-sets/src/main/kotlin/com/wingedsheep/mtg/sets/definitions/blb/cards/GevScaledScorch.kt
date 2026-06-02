package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Gev, Scaled Scorch
 * {B}{R}
 * Legendary Creature — Lizard Mercenary
 * 3/2
 *
 * Ward—Pay 2 life.
 * Other creatures you control enter with an additional +1/+1 counter on them
 * for each opponent who lost life this turn.
 * Whenever you cast a Lizard spell, Gev deals 1 damage to target opponent.
 */
val GevScaledScorch = card("Gev, Scaled Scorch") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Lizard Mercenary"
    oracleText = "Ward—Pay 2 life.\n" +
        "Other creatures you control enter with an additional +1/+1 counter on them " +
        "for each opponent who lost life this turn.\n" +
        "Whenever you cast a Lizard spell, Gev deals 1 damage to target opponent."
    power = 3
    toughness = 2

    keywords(Keyword.WARD)
    keywordAbility(KeywordAbility.wardLife(2))

    // Other creatures you control enter with +1/+1 counters
    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENTS_WHO_LOST_LIFE),
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD
            )
        )
    )

    // Whenever you cast a Lizard spell, deal 1 damage to target opponent
    triggeredAbility {
        trigger = Triggers.YouCastSubtype(Subtype.LIZARD)
        val opponent = target("opponent", Targets.Opponent)
        effect = Effects.DealDamage(1, opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/131ea976-289e-4f32-896d-27bbfd423ba9.jpg?1721427059"
        ruling("2024-07-26", "Gev's second ability cares whether opponents lost life this turn, not how their life totals changed.")
        ruling("2024-07-26", "In the case where Gev and one or more other creatures enter at the same time, those creatures won't enter with additional +1/+1 counters.")
    }
}
