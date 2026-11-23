package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

private const val CONFESSION_TEXT = "When this creature enters, pay any amount of life. Target " +
    "opponent reveals that many cards from their hand. You choose one of them and exile it."

/**
 * Vizkopa Confessor
 * {3}{W}{B}
 * Creature — Human Cleric
 * 1/3
 * Extort
 * When this creature enters, pay any amount of life. Target opponent reveals that many cards from
 * their hand. You choose one of them and exile it.
 *
 * The life payment is [Effects.MayPayAnyAmountOfLife]: the amount is chosen on resolution (0 up to
 * the controller's life total) and bound as X. The printed payment isn't a "may", but 0 is a legal
 * amount and paying 0 reveals and exiles nothing (Gatecrash ruling), which is exactly the gate's
 * decline branch. The rest is the Cabal Interrogator / Taster of Wares reveal shape: the opponent
 * picks which X cards to reveal (all of them when X is at least their hand size), then you pick
 * one of those, and it goes to its owner's exile.
 */
val VizkopaConfessor = card("Vizkopa Confessor") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "$EXTORT_TEXT\n$CONFESSION_TEXT"

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        description = CONFESSION_TEXT
        effect = Effects.MayPayAnyAmountOfLife(
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val revealed = chooseExactly(
                    DynamicAmounts.xValue(),
                    from = hand,
                    chooser = Chooser.TargetPlayer,
                    prompt = "Reveal that many cards from your hand"
                )
                val chosen = chooseExactly(
                    1,
                    from = revealed,
                    chooser = Chooser.Controller,
                    prompt = "Choose one of the revealed cards to exile"
                )
                exile(chosen, opponent.asPlayer)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cac639b9-6c20-45ac-a61f-082bdcebdb83.jpg?1783940098"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
        ruling("2013-01-24", "The amount of life you pay is chosen when Vizkopa Confessor's last ability resolves.")
        ruling("2013-01-24", "If the target opponent is an illegal target when Vizkopa Confessor's last ability tries to resolve, it won't resolve and none of its effects will happen. You won't pay life and no player will reveal any cards.")
        ruling("2013-01-24", "You may pay 0 life when Vizkopa Confessor's \"enters\" ability resolves. If you do, no cards are revealed or exiled.")
    }
}
