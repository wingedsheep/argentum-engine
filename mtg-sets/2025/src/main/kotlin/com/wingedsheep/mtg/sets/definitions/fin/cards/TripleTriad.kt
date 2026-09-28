package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Triple Triad — Final Fantasy #166
 * {3}{R}{R}{R} · Enchantment
 *
 * At the beginning of your upkeep, each player exiles the top card of their library. Until end of
 * turn, you may play the card you own exiled this way and each other card exiled this way with
 * lesser mana value than it without paying their mana costs.
 *
 * Built from impulse atoms. The two exile groups are gathered separately so the "always playable"
 * card (yours) and the MV-filtered set (everyone else's) can be handled independently:
 *  - "mine": your top card → exile.
 *  - "others": each opponent's top card → exile.
 *  - "playableOthers": the others strictly below your card's mana value. MVs are non-negative
 *    integers, so "lesser than it" (< n) is modeled exactly as [CollectionFilter.ManaValueAtMost]
 *    of [DynamicAmount.Subtract](mineMV, 1) — which correctly excludes cards tied with yours.
 *  - Play permission until end of turn, free, is granted over "mine" (unconditionally) and
 *    "playableOthers": [GrantMayPlayFromExileEffect] (also permits lands) + [GrantPlayWithoutPayingCostEffect].
 *
 * Defaults are correct as-is: opponents' cards are cast by you (controller-controls) and go to
 * their owner's graveyard after resolving (no `insteadOfGraveyard` rider). Same gather→exile idiom as
 * Alania's Pathmaker; the per-player [Player.Each]/[Player.EachOpponent] reveal mirrors Psychic Battle.
 */
val TripleTriad = card("Triple Triad") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, each player exiles the top card of their " +
        "library. Until end of turn, you may play the card you own exiled this way and each other " +
        "card exiled this way with lesser mana value than it without paying their mana costs."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            // Your top card → "mine"
            val mine = gather(CardSource.TopOfLibrary(count = 1, player = Player.You))
            exile(mine)
            // Each opponent's top card → "others"
            val others = gather(CardSource.TopOfLibrary(count = 1, player = Player.EachOpponent))
            exile(others)
            // Others with strictly lesser mana value than your card (< mineMV  ==  <= mineMV - 1)
            val playableOthers = filter(
                others,
                GameObjectFilter.Any.manaValueAtMostDynamic(
                    DynamicAmounts.manaValueOf(mine) - 1
                )
            )
            // Until end of turn, play them without paying mana costs.
            run(Effects.GrantMayPlayFromExile(mine, MayPlayExpiry.EndOfTurn))
            run(Effects.GrantPlayWithoutPayingCost(mine))
            run(Effects.GrantMayPlayFromExile(playableOthers, MayPlayExpiry.EndOfTurn))
            run(Effects.GrantPlayWithoutPayingCost(playableOthers))
        }
        description = "At the beginning of your upkeep, each player exiles the top card of their " +
            "library. Until end of turn, you may play the card you own exiled this way and each " +
            "other card exiled this way with lesser mana value than it without paying their mana costs."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9a1de36-7f47-4b28-bb56-38d7e5bed82f.jpg?1782686477"
    }
}
