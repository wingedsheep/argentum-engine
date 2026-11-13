package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UlamogsReclaimer
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
 * Ulamog's Reclaimer (BFZ #68): "When this creature enters, you may put a card an opponent owns
 * from exile into that player's graveyard. If you do, return target instant or sorcery card from
 * your graveyard to your hand."
 *
 * The graveyard target is announced as the trigger goes on the stack — only an instant or sorcery
 * in your own graveyard qualifies; with none, the trigger is removed and nothing is processed. The
 * processed card is picked on resolution. No pick, no return.
 */
class UlamogsReclaimerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UlamogsReclaimer))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(5) { driver.putLandOnBattlefield(you, "Island") }
        return driver to you
    }

    /** Casts the Reclaimer and passes until its trigger asks for a target; returns that prompt. */
    fun GameTestDriver.castReclaimer(you: EntityId): ChooseTargetsDecision {
        castSpell(you, putCardInHand(you, "Ulamog's Reclaimer"))
        var safety = 0
        while (pendingDecision !is ChooseTargetsDecision && safety++ < 20) bothPass()
        return pendingDecision as ChooseTargetsDecision
    }

    /**
     * Resolves the trigger, answering the processor pick with [pick] (empty to decline). Returns
     * the options the pick offered, or null when no pick was asked.
     */
    fun GameTestDriver.resolveTrigger(you: EntityId, pick: List<EntityId>): List<EntityId>? {
        var offered: List<EntityId>? = null
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 20) {
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

    test("processing an opponent's exiled card returns the target instant to your hand") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt")
        val myBears = driver.putCardInGraveyard(you, "Grizzly Bears")
        val theirBolt = driver.putCardInGraveyard(opponent, "Lightning Bolt")
        val exiled = driver.putCardInExile(opponent, "Island")

        val prompt = driver.castReclaimer(you)
        val legal = prompt.legalTargets.values.flatten()
        legal shouldContain bolt
        legal shouldNotContain myBears
        legal shouldNotContain theirBolt

        driver.submitTargetSelection(you, listOf(bolt))
        val offered = driver.resolveTrigger(you, listOf(exiled))

        offered!! shouldContainExactly listOf(exiled)
        driver.getGraveyard(opponent) shouldContain exiled
        driver.state.getZone(opponent, Zone.EXILE) shouldNotContain exiled
        driver.getHand(you) shouldContain bolt
        driver.getGraveyard(you) shouldNotContain bolt
    }

    test("declining the pick leaves the exiled card in exile and the instant in the graveyard") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bolt = driver.putCardInGraveyard(you, "Lightning Bolt")
        val exiled = driver.putCardInExile(opponent, "Island")

        driver.castReclaimer(you)
        driver.submitTargetSelection(you, listOf(bolt))
        driver.resolveTrigger(you, emptyList())

        driver.state.getZone(opponent, Zone.EXILE) shouldContain exiled
        driver.getGraveyard(you) shouldContain bolt
        driver.getHand(you) shouldNotContain bolt
    }

    test("with no instant or sorcery in your graveyard the trigger has no target and processes nothing") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        driver.putCardInGraveyard(opponent, "Lightning Bolt")
        val exiled = driver.putCardInExile(opponent, "Island")

        driver.castSpell(you, driver.putCardInHand(you, "Ulamog's Reclaimer"))
        var sawPrompt = false
        var safety = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && safety++ < 20) {
            when (driver.pendingDecision) {
                null -> driver.bothPass()
                else -> { sawPrompt = true; break }
            }
        }

        sawPrompt shouldBe false
        driver.state.stack.isEmpty() shouldBe true
        driver.state.getZone(opponent, Zone.EXILE) shouldContain exiled
    }
})
