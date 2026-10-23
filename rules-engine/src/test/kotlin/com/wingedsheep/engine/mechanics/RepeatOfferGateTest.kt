package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `RepeatCondition.PlayerChooses.onlyIf` (the `repeatProcess { }` pipeline verb): "you may repeat"
 * is offered only after a pass where the gate holds, read against that pass's own outputs; a pass
 * where it fails ends the loop without a prompt. Delina, Wild Mage's "15—20 | … You may roll
 * again." is the motivating card.
 *
 * Covers both seams a pass's outputs reach the gate through: a synchronous body (the result's
 * stored numbers) and a body that pauses for a card choice after storing its number (the
 * continuation's `bodyNumbers`).
 */
class RepeatOfferGateTest : FunSpec({

    // Synchronous body: mill a card; you may repeat while fewer than three cards are in your graveyard.
    val millWhileShort = card("Mill While Short") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Mill a card. If fewer than three cards are in your graveyard, you may repeat this process."
        spell {
            effect = Effects.Pipeline {
                repeatProcess {
                    mill(1)
                    val graveyard = storeNumber(DynamicAmount.Count(Player.You, Zone.GRAVEYARD))
                    RepeatCondition.PlayerChooses(
                        decider = EffectTarget.Controller,
                        prompt = "Mill again?",
                        onlyIf = Conditions.CompareAmounts(graveyard.amount, ComparisonOperator.LT, 3),
                    )
                }
            }
        }
    }

    // Pausing body: note your hand size, then exile up to one card from it; if you had two or more
    // cards before this pass, you may repeat.
    val exileWhileFull = card("Exile While Full") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Exile up to one card from your hand. If you had two or more cards in hand, you may repeat this process."
        spell {
            effect = Effects.Pipeline {
                repeatProcess {
                    val handSize = storeNumber(DynamicAmount.Count(Player.You, Zone.HAND))
                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                    exile(chooseUpTo(1, from = hand, alwaysPrompt = true))
                    RepeatCondition.PlayerChooses(
                        decider = EffectTarget.Controller,
                        prompt = "Exile another card?",
                        onlyIf = Conditions.CompareAmounts(handSize.amount, ComparisonOperator.GTE, 2),
                    )
                }
            }
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(millWhileShort, exileWhileFull))
        initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the offer follows each pass's gate and stops unprompted once it fails") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Mill While Short")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, spell)
        d.bothPass()

        withClue("one card milled: offered") {
            d.getGraveyard(me).size shouldBe 1
            d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.submitYesNo(me, true)
        withClue("two cards milled: offered again") {
            d.getGraveyard(me).size shouldBe 2
            d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.submitYesNo(me, true)
        withClue("three cards milled: the gate fails, so the loop ends with no prompt") {
            d.state.pendingDecision shouldBe null
            d.getGraveyardCardNames(me) shouldBe listOf("Forest", "Forest", "Forest", "Mill While Short")
        }
    }

    test("declining an open offer ends the loop") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Mill While Short")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, spell)
        d.bothPass()
        d.submitYesNo(me, false)

        d.state.pendingDecision shouldBe null
        d.getGraveyardCardNames(me) shouldBe listOf("Forest", "Mill While Short")
    }

    test("a number stored before the body pauses still reaches the gate") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Exile While Full")
        d.getHand(me).filter { it != spell }.forEach { d.moveToGraveyard(it) }
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, spell)
        d.bothPass()

        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(bears))
        withClue("two cards in hand before the pass: offered") {
            d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.submitYesNo(me, true)
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(bolt))
        withClue("one card in hand before the pass: no offer") {
            d.state.pendingDecision shouldBe null
            d.getExileCardNames(me) shouldBe listOf("Grizzly Bears", "Lightning Bolt")
        }
    }
})
