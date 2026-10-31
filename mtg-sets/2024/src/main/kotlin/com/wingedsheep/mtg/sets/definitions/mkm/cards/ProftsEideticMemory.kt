package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Proft's Eidetic Memory — Murders at Karlov Manor #67
 * {1}{U} · Legendary Enchantment · Rare
 *
 * When Proft's Eidetic Memory enters, draw a card.
 * You have no maximum hand size.
 * At the beginning of combat on your turn, if you've drawn more than one card this turn, put X
 * +1/+1 counters on target creature you control, where X is the number of cards you've drawn this
 * turn minus one.
 *
 * A two-mana enchantment that turns any second draw into a combat trick you never have to hold up
 * mana for. It replaces itself, so the turn it lands it has already done half the work of switching
 * itself on for *next* turn.
 *
 * Three abilities, three plain rails. The interesting one is the third, and it hinges on the
 * distinction between the gate and the amount:
 *
 * - **The gate** is `interveningIf` ([Conditions.YouDrewCardsThisTurn]`(2)` — "more than one" is
 *   "two or more"), not a `triggerRestriction`. That matters in both directions per CR 603.4: the
 *   ability doesn't even go on the stack if the count isn't there at the start of combat (the first
 *   printed ruling), and it is re-checked on resolution, so a Stifle-adjacent effect that somehow
 *   un-drew a card would still fizzle it.
 * - **The amount** is read separately, once, as the ability resolves (the third ruling), so a draw
 *   made in response to the trigger *does* grow X even though it came too late to matter for the
 *   gate. `DynamicAmount.Subtract(TurnTracking(You, CARDS_DRAWN), Fixed(1))` is exactly that:
 *   the same per-player `CardsDrawnThisTurnComponent` the gate reads, evaluated at resolution.
 *
 * The tracker is a turn-scoped count, not a "since this entered" one, so the second ruling falls
 * out for free — draws made before the enchantment hit the battlefield are already in the tally.
 *
 * `Triggers.you.beginningOf(Step.BEGIN_COMBAT)` is already scoped to `Step.BEGIN_COMBAT, Player.You`, so "on your turn"
 * needs no extra condition.
 */
val ProftsEideticMemory = card("Proft's Eidetic Memory") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment"
    oracleText = "When Proft's Eidetic Memory enters, draw a card.\n" +
        "You have no maximum hand size.\n" +
        "At the beginning of combat on your turn, if you've drawn more than one card this turn, " +
        "put X +1/+1 counters on target creature you control, where X is the number of cards " +
        "you've drawn this turn minus one."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "When Proft's Eidetic Memory enters, draw a card."
    }

    staticAbility {
        ability = NoMaximumHandSize()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouDrewCardsThisTurn(2)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.cardsDrawnThisTurn(Player.You) - 1,
            creature,
        )
        description = "At the beginning of combat on your turn, if you've drawn more than one " +
            "card this turn, put X +1/+1 counters on target creature you control, where X is the " +
            "number of cards you've drawn this turn minus one."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Julie Dillon"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af5b29b3-974c-4200-8df8-b072c11e1600.jpg?1783912906"

        ruling(
            "2024-02-02",
            "If you haven't drawn more than one card by the time a beginning of combat step " +
                "begins on your turn, Proft's Eidetic Memory's last ability won't trigger at all."
        )
        ruling(
            "2024-02-02",
            "Proft's Eidetic Memory's last ability looks at how many cards you've drawn this " +
                "turn, even if it wasn't on the battlefield when you drew those cards."
        )
        ruling(
            "2024-02-02",
            "The value of X is determined only once, as Proft's Eidetic Memory's last ability " +
                "resolves."
        )
    }
}
