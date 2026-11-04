package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angrath, the Flame-Chained
 * {3}{B}{R}
 * Legendary Planeswalker — Angrath
 * Starting Loyalty: 4
 *
 * +1: Each opponent discards a card and loses 2 life.
 * −3: Gain control of target creature until end of turn. Untap it. It gains haste until end of turn.
 * Sacrifice it at the beginning of the next end step if it has mana value 3 or less.
 * −8: Each opponent loses life equal to the number of cards in their graveyard.
 *
 * The +1's life loss doesn't depend on the discard (ruling): an opponent with an empty hand still
 * loses 2.
 *
 * The −3 is the Unexpected Request threaten package followed by the Berserk-shaped delayed trigger:
 * the trigger watches the stolen creature and checks its mana value only as it resolves in the end
 * step (ruling), via [Conditions.EntityMatches] inside [Effects.If] — not an intervening "if".
 * [Effects.SacrificeTarget] only sacrifices a permanent Angrath's controller still controls, so a
 * creature that has left your control by then is left alone.
 *
 * The −8 runs [Effects.ForEachPlayer] over each opponent; the loop rebinds `Player.You` to the
 * opponent being processed, so the count reads *that* player's own graveyard.
 */
val AngrathTheFlameChained = card("Angrath, the Flame-Chained") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Angrath"
    startingLoyalty = 4
    oracleText = "+1: Each opponent discards a card and loses 2 life.\n" +
        "−3: Gain control of target creature until end of turn. Untap it. It gains haste until end of " +
        "turn. Sacrifice it at the beginning of the next end step if it has mana value 3 or less.\n" +
        "−8: Each opponent loses life equal to the number of cards in their graveyard."

    // +1: Each opponent discards a card and loses 2 life.
    loyaltyAbility(+1) {
        effect = Effects.EachOpponentDiscards(1) then
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // −3: Threaten, with a mana-value-checked sacrifice at the beginning of the next end step.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature, Duration.EndOfTurn) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                watchedTarget = creature,
                effect = Effects.If(
                    Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.manaValueAtMost(3)),
                    Effects.SacrificeTarget(EffectTarget.TriggeringEntity)
                )
            )
    }

    // −8: Each opponent loses life equal to the number of cards in their graveyard.
    loyaltyAbility(-8) {
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effects = listOf(
                Effects.LoseLife(
                    amount = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).count(),
                    target = EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4d76d03-4fcf-42f8-8c2e-ad6b03d58677.jpg?1783935278"
        ruling(
            "2018-01-19",
            "Angrath's first ability causes each opponent to lose 2 life even if some or all of those " +
                "players were unable to discard a card.",
        )
        ruling(
            "2018-01-19",
            "You can target and gain control of an untapped creature with Angrath's second ability. You " +
                "can also untap a creature you already control and give it haste.",
        )
        ruling(
            "2018-01-19",
            "Whether the target creature has mana value 3 or less is checked only as the delayed triggered " +
                "ability from Angrath's second ability resolves during the end step.",
        )
    }
}
