package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.Mindreaver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mindreaver (BNG #44) — heroic exiles the top three cards of target player's library, linked to
 * Mindreaver; "{U}{U}, Sacrifice this creature: Counter target spell with the same name as a card
 * exiled with this creature."
 *
 * The point of these tests is the sacrifice: the counter's target is chosen while Mindreaver is on
 * the battlefield, but re-checked on resolution (CR 608.2b) after the sacrifice cost moved it to
 * the graveyard. The pile it reads there must still be the one its heroic ability built — and,
 * per the card's ruling, never another Mindreaver's.
 */
class MindreaverScenarioTest : FunSpec({

    val counterId = Mindreaver.script.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Mindreaver)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Player 1 casts Giant Growth on [mindreaver]; its heroic trigger targets the opponent and
     * resolves, leaving Giant Growth on the stack. Then the opponent, with priority, casts
     * [oppSpell] at player 1, and passes back.
     */
    fun GameTestDriver.heroicThenOpponentCasts(mindreaver: EntityId, oppSpell: String): EntityId {
        val me = player1
        val opp = player2
        val growth = putCardInHand(me, "Giant Growth")
        giveMana(me, Color.GREEN, 1)
        castSpell(me, growth, listOf(mindreaver)).error shouldBe null
        val decision = pendingDecision
        if (decision is ChooseTargetsDecision) submitTargetSelection(me, listOf(opp))
        bothPass() // heroic trigger resolves; Giant Growth stays on the stack

        val spell = putCardInHand(opp, oppSpell)
        giveMana(opp, Color.RED, 1)
        giveMana(opp, Color.GREEN, 1)
        passPriority(me)
        submit(
            CastSpell(
                playerId = opp,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(me)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        passPriority(opp)
        return spell
    }

    test("heroic exiles three; the sacrificed Mindreaver still counters a spell named like one of them") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val mindreaver = d.putPermanentOnBattlefield(me, "Mindreaver")
        d.putCardOnTopOfLibrary(opp, "Lightning Bolt")

        val bolt = d.heroicThenOpponentCasts(mindreaver, "Lightning Bolt")
        withClue("heroic exiled the opponent's top three cards") {
            d.getExileCardNames(opp).size shouldBe 3
            d.getExileCardNames(opp).contains("Lightning Bolt") shouldBe true
        }

        d.priorityPlayer shouldBe me
        d.giveMana(me, Color.BLUE, 2)
        val activation = d.submit(
            ActivateAbility(me, mindreaver, counterId, targets = listOf(ChosenTarget.Spell(bolt)))
        )
        withClue("activation targeting a same-named spell is legal: ${activation.error}") {
            activation.error shouldBe null
        }
        withClue("Mindreaver was sacrificed as a cost") {
            d.findPermanent(me, "Mindreaver") shouldBe null
        }

        d.bothPass() // the counter resolves with Mindreaver already in the graveyard
        withClue("the Bolt was countered, not fizzled on an empty pile") {
            d.getGraveyardCardNames(opp).contains("Lightning Bolt") shouldBe true
            d.getStackSpellNames().contains("Lightning Bolt") shouldBe false
        }
        d.bothPass() // Giant Growth fizzles (Mindreaver is gone)
        d.getLifeTotal(me) shouldBe 20
    }

    test("a spell whose name matches no exiled card can't be targeted") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val mindreaver = d.putPermanentOnBattlefield(me, "Mindreaver")
        d.putCardOnTopOfLibrary(opp, "Giant Growth")

        val bolt = d.heroicThenOpponentCasts(mindreaver, "Lightning Bolt")
        d.giveMana(me, Color.BLUE, 2)
        val activation = d.submit(
            ActivateAbility(me, mindreaver, counterId, targets = listOf(ChosenTarget.Spell(bolt)))
        )
        withClue("Lightning Bolt isn't named like a card exiled with Mindreaver") {
            activation.error shouldNotBe null
        }
        d.findPermanent(me, "Mindreaver") shouldNotBe null
    }

    test("another Mindreaver doesn't refer to the cards exiled with the first") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val first = d.putPermanentOnBattlefield(me, "Mindreaver")
        val second = d.putPermanentOnBattlefield(me, "Mindreaver")
        d.putCardOnTopOfLibrary(opp, "Lightning Bolt")

        val bolt = d.heroicThenOpponentCasts(first, "Lightning Bolt")
        d.giveMana(me, Color.BLUE, 2)
        val activation = d.submit(
            ActivateAbility(me, second, counterId, targets = listOf(ChosenTarget.Spell(bolt)))
        )
        withClue("the second Mindreaver's pile is empty") {
            activation.error shouldNotBe null
        }
    }
})
