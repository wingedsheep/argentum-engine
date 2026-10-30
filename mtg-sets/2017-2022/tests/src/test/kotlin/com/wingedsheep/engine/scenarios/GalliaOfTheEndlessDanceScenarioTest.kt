package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gallia of the Endless Dance (THB #217).
 *
 * "Whenever you attack with three or more creatures, you may discard a card at random. If you do,
 * draw two cards."
 *
 * The draw is gated on a card actually being discarded: with an empty hand nothing is discarded and
 * nothing is drawn (ruling).
 */
class GalliaOfTheEndlessDanceScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attackers(count: Int): List<EntityId> {
        val gallia = putCreatureOnBattlefield(player1, "Gallia of the Endless Dance")
        val bears = (1 until count).map { putCreatureOnBattlefield(player1, "Grizzly Bears") }
        return (listOf(gallia) + bears).onEach(::removeSummoningSickness)
    }

    fun GameTestDriver.attackWith(creatures: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        val opponent = getOpponent(player1)
        declareAttackers(player1, creatures.associateWith { opponent }).error shouldBe null
    }

    test("attacking with three creatures: discarding at random draws two cards") {
        val d = driver()
        val creatures = d.attackers(3)
        val handBefore = d.getHandSize(d.player1)
        val graveyardBefore = d.getGraveyard(d.player1).size

        d.attackWith(creatures)
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)

        d.getGraveyard(d.player1).size shouldBe graveyardBefore + 1
        d.getHandSize(d.player1) shouldBe handBefore - 1 + 2
    }

    test("declining the discard draws nothing") {
        val d = driver()
        val creatures = d.attackers(3)
        val handBefore = d.getHandSize(d.player1)

        d.attackWith(creatures)
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, false)

        d.getHandSize(d.player1) shouldBe handBefore
    }

    test("with an empty hand nothing is discarded, so nothing is drawn") {
        val d = driver()
        val creatures = d.attackers(3)
        d.getHand(d.player1).forEach(d::moveToGraveyard)
        d.getHandSize(d.player1) shouldBe 0

        d.attackWith(creatures)
        d.bothPass()
        if (d.pendingDecision is YesNoDecision) d.submitYesNo(d.player1, true)

        withClue("no card was discarded, so the 'if you do' draw doesn't happen") {
            d.getHandSize(d.player1) shouldBe 0
        }
    }

    test("attacking with only two creatures doesn't trigger") {
        val d = driver()
        val creatures = d.attackers(2)
        val handBefore = d.getHandSize(d.player1)

        d.attackWith(creatures)

        d.state.stack.shouldBeEmpty()
        d.getHandSize(d.player1) shouldBe handBefore
    }
})
