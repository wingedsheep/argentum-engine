package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invoke Despair — Kamigawa: Neon Dynasty #101 (canonical printing)
 * {1}{B}{B}{B}{B} · Sorcery
 *
 * Target opponent sacrifices a creature of their choice. If they can't, they lose 2 life and you
 * draw a card. Then repeat this process for an enchantment and a planeswalker.
 *
 * "If they can't" is read off the sacrifice that just happened: each round freezes the running
 * "sacrificed this way" count, has the opponent sacrifice, and fires the fallback when that count
 * didn't move. The count accumulates across the whole resolution, so comparing against the frozen
 * value (rather than against zero) keeps a creature sacrificed in round one from masking an empty
 * board in round two. All three rounds always happen, against the same opponent.
 */
val InvokeDespair = card("Invoke Despair") {
    manaCost = "{1}{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent sacrifices a creature of their choice. If they can't, they lose 2 life " +
        "and you draw a card. Then repeat this process for an enchantment and a planeswalker."

    spell {
        val opponent = target(Targets.Opponent)
        effect = despairRound("creature", GameObjectFilter.Creature, opponent) then
            despairRound("enchantment", GameObjectFilter.Enchantment, opponent) then
            despairRound("planeswalker", GameObjectFilter.Planeswalker, opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Olivier Bernard"
        flavorText = "Although officials said it was a sewer failure, the people whispered that it was " +
            "a warning of night's reach."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35af9d5c-4449-4549-b549-c3ba4a67dee0.jpg?1783923885"
        ruling(
            "2022-02-18",
            "The target opponent doesn't choose whether to sacrifice a creature or not, although they do " +
                "choose which creature they control to sacrifice."
        )
        ruling(
            "2022-02-18",
            "When you repeat the process, you don't choose a new player. The same player sacrifices each " +
                "of the three permanents, if able."
        )
        ruling(
            "2022-02-18",
            "You repeat the process for an enchantment and a planeswalker whether the target opponent was " +
                "able to sacrifice a creature or not."
        )
    }
}

/** One round: the opponent sacrifices a [filter] permanent; if nothing was sacrificed, they lose 2 and you draw. */
private fun despairRound(key: String, filter: GameObjectFilter, opponent: EffectTarget): Effect {
    val before = "invokeDespair_${key}_before"
    return Effects.StoreNumber(before, DynamicAmounts.permanentsSacrificedThisWay()) then
        Effects.Sacrifice(filter = filter, target = opponent) then
        Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.permanentsSacrificedThisWay(),
                ComparisonOperator.EQ,
                DynamicAmounts.storedNumber(before),
            ),
            then = Effects.LoseLife(2, opponent) then Effects.DrawCards(1),
        )
}
