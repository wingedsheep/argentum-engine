package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kambal, Profiteering Mayor
 * {1}{W}{B}
 * Legendary Creature — Human Advisor
 * 2/4
 *
 * Whenever one or more tokens your opponents control enter, for each of them, create a tapped token
 * that's a copy of it. This ability triggers only once each turn.
 * Whenever one or more tokens you control enter, each opponent loses 1 life and you gain 1 life.
 *
 * Implementation notes:
 * - Ability 1 is the opponent-scoped batched ETB trigger
 *   ([Triggers.OneOrMoreOpponentPermanentsEnter] on [GameObjectFilter.Token]). The batch exposes its
 *   matching tokens to the payoff as the pipeline collection `IterationSpace.TRIGGER_CAPTURED_COLLECTION`,
 *   so [ForEachInCollectionEffect] iterates them and [CreateTokenCopyOfTargetEffect] (target
 *   [EffectTarget.Self], `tapped = true`) makes one tapped copy of each. `oncePerTurn = true` gives
 *   "This ability triggers only once each turn" (CR 603.3 / engine-tracked once-per-turn). Per the
 *   official rulings, the copies use each original token's copiable characteristics and enter tapped;
 *   the copy executor reads each token at resolution, so any that left the battlefield meanwhile
 *   simply no-op.
 * - Ability 2 is the you-scoped batched ETB trigger ([Triggers.OneOrMorePermanentsEnter] on
 *   [GameObjectFilter.Token], which defaults to "you control"); it drains each opponent for 1 and
 *   gains you 1, firing once per batch with no per-turn limit.
 */
val KambalProfiteeringMayor = card("Kambal, Profiteering Mayor") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 4
    oracleText = "Whenever one or more tokens your opponents control enter, for each of them, " +
        "create a tapped token that's a copy of it. This ability triggers only once each turn.\n" +
        "Whenever one or more tokens you control enter, each opponent loses 1 life and you gain 1 life."

    // Whenever one or more tokens your opponents control enter, for each of them, create a tapped
    // copy. Triggers only once each turn.
    triggeredAbility {
        trigger = Triggers.OneOrMoreOpponentPermanentsEnter(GameObjectFilter.Token)
        oncePerTurn = true
        effect = ForEachInCollectionEffect(
            collection = IterationSpace.TRIGGER_CAPTURED_COLLECTION,
            effect = CreateTokenCopyOfTargetEffect(target = EffectTarget.Self, tapped = true)
        )
    }

    // Whenever one or more tokens you control enter, each opponent loses 1 life and you gain 1 life.
    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Token)
        effect = Effects.Composite(
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d53a775d-5898-41a8-b404-9b7d4721c6ba.jpg?1712356126"
        ruling("2024-04-12", "Each of the new tokens copies the original characteristics of the appropriate token as stated by the effect that created that token and nothing else (unless that creature is copying something else). It doesn't copy whether the opponent's token is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2024-04-12", "If the copied token has {X} in its mana cost, X is 0. (Most tokens don't have a mana cost unless they're copying something else.)")
        ruling("2024-04-12", "If the copied token is copying something else, then the new token enters the battlefield as whatever that token copied.")
        ruling("2024-04-12", "Any enters-the-battlefield abilities of the copied token will trigger when the token enters the battlefield. Any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities of the copied token will also work.")
    }
}
