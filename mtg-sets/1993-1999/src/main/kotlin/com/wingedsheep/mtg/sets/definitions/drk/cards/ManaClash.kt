package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mana Clash
 * {R}
 * Sorcery
 * You and target opponent each flip a coin. Mana Clash deals 1 damage to each player whose coin
 * comes up tails. Repeat this process until both players' coins come up heads on the same flip.
 *
 * A do-while loop: the body flips once for each player, burns whoever flipped tails, and the repeat
 * condition asks whether *this* flip was double-heads. `RepeatWhile` runs the body at least once and
 * evaluates the condition after each iteration, which is exactly "repeat this process until…".
 *
 * Two separate one-coin flips rather than `FlipCoins(2)`: the card cares which player each result
 * belongs to, and a two-coin heads *count* cannot distinguish "I flipped heads, they flipped tails"
 * from the reverse. Each flip stores its own heads count, so a stored 0 means that player's coin
 * came up tails.
 *
 * The loop ends only on a simultaneous double-heads, so a player who flips heads still keeps
 * flipping while their opponent misses — which is the card's whole reputation.
 */
val ManaClash = card("Mana Clash") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You and target opponent each flip a coin. Mana Clash deals 1 damage to each " +
        "player whose coin comes up tails. Repeat this process until both players' coins come up " +
        "heads on the same flip."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        val myHeads = DynamicAmount.VariableReference("manaClashMine")
        val theirHeads = DynamicAmount.VariableReference("manaClashTheirs")

        effect = Effects.RepeatWhile(
            body = Effects.Composite(
                Effects.FlipCoins(1, storeHeadsAs = "manaClashMine"),
                Effects.FlipCoins(1, storeHeadsAs = "manaClashTheirs"),
                ConditionalEffect(
                    condition = Conditions.CompareAmounts(
                        myHeads, ComparisonOperator.EQ, DynamicAmount.Fixed(0)
                    ),
                    effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)),
                ),
                ConditionalEffect(
                    condition = Conditions.CompareAmounts(
                        theirHeads, ComparisonOperator.EQ, DynamicAmount.Fixed(0)
                    ),
                    effect = Effects.DealDamage(1, opponent),
                ),
            ),
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.Not(
                    Conditions.All(
                        Conditions.CompareAmounts(myHeads, ComparisonOperator.EQ, DynamicAmount.Fixed(1)),
                        Conditions.CompareAmounts(theirHeads, ComparisonOperator.EQ, DynamicAmount.Fixed(1)),
                    )
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72955141-d990-459f-adbe-7d3d0f5f6c95.jpg?1783947933"
    }
}
