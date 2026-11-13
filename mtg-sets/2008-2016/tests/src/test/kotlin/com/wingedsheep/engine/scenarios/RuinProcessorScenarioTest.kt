package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.RuinProcessor
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
 * Ruin Processor (BFZ #12): "When you cast this spell, you may put a card an opponent owns from
 * exile into that player's graveyard. If you do, you gain 5 life."
 *
 * A cast trigger: it resolves while Ruin Processor is still on the stack. Only a card an opponent
 * owns can be picked, and no pick means no life.
 */
class RuinProcessorScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RuinProcessor))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(7) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    /**
     * Casts Ruin Processor and resolves only its cast trigger, answering the processor pick with
     * [pick] (empty to decline). Returns the options the pick offered, or null when none was asked.
     * The creature spell is left on the stack.
     */
    fun GameTestDriver.castAndResolveTrigger(you: EntityId, pick: List<EntityId>): List<EntityId>? {
        castSpell(you, putCardInHand(you, "Ruin Processor")).error shouldBe null
        state.stack.size shouldBe 2
        var offered: List<EntityId>? = null
        var safety = 0
        while ((state.stack.size > 1 || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is SelectCardsDecision -> {
                    offered = decision.options
                    submitCardSelection(you, pick)
                }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        return offered
    }

    test("processing an opponent's exiled card gains 5 life before the creature resolves") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val exiled = driver.putCardInExile(opponent, "Swamp")
        val startingLife = driver.getLifeTotal(you)

        val offered = driver.castAndResolveTrigger(you, listOf(exiled))

        offered!! shouldContainExactly listOf(exiled)
        driver.getGraveyard(opponent) shouldContain exiled
        driver.state.getZone(opponent, Zone.EXILE) shouldNotContain exiled
        driver.getLifeTotal(you) shouldBe startingLife + 5
        // The creature spell is still waiting on the stack.
        driver.state.stack.size shouldBe 1
    }

    test("declining the pick leaves the exiled card and gains no life") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val exiled = driver.putCardInExile(opponent, "Swamp")
        val startingLife = driver.getLifeTotal(you)

        driver.castAndResolveTrigger(you, emptyList())

        driver.state.getZone(opponent, Zone.EXILE) shouldContain exiled
        driver.getLifeTotal(you) shouldBe startingLife
    }

    test("a card you own in exile can't be processed, so no life is gained") {
        val (driver, you) = setup()
        val mine = driver.putCardInExile(you, "Swamp")
        val startingLife = driver.getLifeTotal(you)

        val offered = driver.castAndResolveTrigger(you, emptyList())

        offered shouldBe null
        driver.state.getZone(you, Zone.EXILE) shouldContain mine
        driver.getLifeTotal(you) shouldBe startingLife
    }
})
