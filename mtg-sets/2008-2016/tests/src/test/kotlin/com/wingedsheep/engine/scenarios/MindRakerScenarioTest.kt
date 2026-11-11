package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.MindRaker
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Mind Raker (BFZ #95): "When this creature enters, you may put a card an opponent owns from exile
 * into that player's graveyard. If you do, each opponent discards a card."
 *
 * The processed card is picked on resolution, and only a card an opponent owns can be picked.
 * No pick, no discard.
 */
class MindRakerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MindRaker))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(4) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    /**
     * Casts Mind Raker and resolves it and its trigger, answering the processor pick with [pick]
     * (empty to decline) and the opponent's discard with their first card. Returns the options the
     * processor pick offered, or null when no pick was asked.
     */
    fun GameTestDriver.castAndResolve(you: EntityId, pick: List<EntityId>): List<EntityId>? {
        castSpell(you, putCardInHand(you, "Mind Raker"))
        var offered: List<EntityId>? = null
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is SelectCardsDecision ->
                    if (decision.playerId == you) {
                        offered = decision.options
                        submitCardSelection(you, pick)
                    } else {
                        submitCardSelection(decision.playerId, decision.options.take(maxOf(decision.minSelections, 1)))
                    }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        return offered
    }

    test("processing an opponent's exiled card makes each opponent discard a card") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val exiled = driver.putCardInExile(opponent, "Swamp")
        val handBefore = driver.getHandSize(opponent)
        val graveyardBefore = driver.getGraveyard(opponent).size

        val offered = driver.castAndResolve(you, listOf(exiled))

        offered!! shouldContainExactly listOf(exiled)
        driver.getGraveyard(opponent) shouldContain exiled
        driver.state.getZone(opponent, Zone.EXILE) shouldNotContain exiled
        driver.getHandSize(opponent) shouldBe handBefore - 1
        driver.getGraveyard(opponent).size shouldBe graveyardBefore + 2
    }

    test("declining the pick leaves the exiled card and the opponent's hand untouched") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val exiled = driver.putCardInExile(opponent, "Swamp")
        val handBefore = driver.getHandSize(opponent)

        driver.castAndResolve(you, emptyList())

        driver.state.getZone(opponent, Zone.EXILE) shouldContain exiled
        driver.getHandSize(opponent) shouldBe handBefore
    }

    test("a card you own in exile can't be processed, so no one discards") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val mine = driver.putCardInExile(you, "Swamp")
        val handBefore = driver.getHandSize(opponent)

        val offered = driver.castAndResolve(you, emptyList())

        offered shouldBe null
        driver.state.getZone(you, Zone.EXILE) shouldContain mine
        driver.getHandSize(opponent) shouldBe handBefore
    }
})
