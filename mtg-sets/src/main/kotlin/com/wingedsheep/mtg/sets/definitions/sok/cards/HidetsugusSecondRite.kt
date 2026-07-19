package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hidetsugu's Second Rite
 * {3}{R}
 * Instant
 * If target player has exactly 10 life, Hidetsugu's Second Rite deals 10 damage to that player.
 *
 * Not an intervening-if — the spell targets a player at cast time and simply checks their life
 * total when it resolves. If they aren't at exactly 10, the spell resolves doing nothing (it does
 * not fizzle for having no legal target). Modeled as a [ConditionalEffect] whose gate compares the
 * targeted player's current life ([DynamicAmount.LifeTotal] of [Player.ContextPlayer]`(0)` — the
 * single player target) against 10.
 *
 * Canonical earliest printing: Saviors of Kamigawa (2005). Reprinted in Foundations (2024) — see
 * the `fdn` package's Printing row.
 */
val HidetsugusSecondRite = card("Hidetsugu's Second Rite") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "If target player has exactly 10 life, Hidetsugu's Second Rite deals 10 damage to that player."

    spell {
        val targetPlayer = target("target player", Targets.Player)
        effect = ConditionalEffect(
            condition = Conditions.CompareAmounts(
                DynamicAmount.LifeTotal(Player.ContextPlayer(0)),
                ComparisonOperator.EQ,
                DynamicAmount.Fixed(10),
            ),
            effect = Effects.DealDamage(10, targetPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Jeff Miracola"
        flavorText = "Hidetsugu never relinquished a grudge. He let it burn within him, gathering ever greater intensity until the final moment of vengeance."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e48eb77-3bd7-444a-9262-799cc706c05a.jpg?1783944146"
    }
}
