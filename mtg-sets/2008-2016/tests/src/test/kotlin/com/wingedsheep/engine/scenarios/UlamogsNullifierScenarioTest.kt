package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UlamogsNullifier
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ulamog's Nullifier (BFZ #207): "Flash. Flying. When this creature enters, you may put two cards
 * your opponents own from exile into their owners' graveyards. If you do, counter target spell."
 *
 * Flashed in over an opponent's Grizzly Bears: the trigger targets the Bears as it goes on the
 * stack, and the counter only happens if exactly two of the opponent's exiled cards are processed.
 * With one card an opponent owns in exile the option isn't offered and the Bears resolve; declining
 * leaves both the exile and the Bears alone.
 */
class UlamogsNullifierScenarioTest : FunSpec({

    /** Returns (driver, you, opponent): the opponent is the active player and holds the Bears. */
    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UlamogsNullifier))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)
        repeat(2) { driver.putLandOnBattlefield(opponent, "Forest") }
        repeat(2) { driver.putLandOnBattlefield(you, "Island") }
        repeat(2) { driver.putLandOnBattlefield(you, "Swamp") }
        return Triple(driver, you, opponent)
    }

    class TriggerLog(var askedYesNo: Boolean = false, var minPick: Int? = null, var maxPick: Int? = null)

    /**
     * The opponent casts Grizzly Bears, you flash in the Nullifier, and the stack resolves fully,
     * answering the trigger's target with the Bears, the yes/no with [accept], and the pick with [pick].
     */
    fun GameTestDriver.flashOverBears(
        you: EntityId,
        opponent: EntityId,
        accept: Boolean,
        pick: List<EntityId>,
    ): Pair<EntityId, TriggerLog> {
        val bears = putCardInHand(opponent, "Grizzly Bears")
        castSpell(opponent, bears).error shouldBe null
        passPriority(opponent)
        castSpell(you, putCardInHand(you, "Ulamog's Nullifier")).error shouldBe null
        state.stack.size shouldBe 2

        val log = TriggerLog()
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 30) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision -> {
                    decision.legalTargets.values.flatten() shouldContain bears
                    submitTargetSelection(you, listOf(bears))
                }
                is YesNoDecision -> {
                    log.askedYesNo = true
                    submitYesNo(you, accept)
                }
                is SelectCardsDecision -> {
                    log.minPick = decision.minSelections
                    log.maxPick = decision.maxSelections
                    submitCardSelection(you, pick)
                }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        return bears to log
    }

    test("processing two of the opponent's exiled cards counters the target spell") {
        val (driver, you, opponent) = setup()
        val first = driver.putCardInExile(opponent, "Forest")
        val second = driver.putCardInExile(opponent, "Lightning Bolt")
        val third = driver.putCardInExile(opponent, "Forest")

        val (_, log) = driver.flashOverBears(you, opponent, accept = true, pick = listOf(first, second))

        log.askedYesNo shouldBe true
        log.minPick shouldBe 2
        log.maxPick shouldBe 2
        driver.getGraveyard(opponent) shouldContainAll listOf(first, second)
        driver.state.getZone(opponent, Zone.EXILE) shouldContain third
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(you, "Ulamog's Nullifier") shouldNotBe null
    }

    test("with only one card an opponent owns in exile, nothing is processed and the spell resolves") {
        val (driver, you, opponent) = setup()
        val only = driver.putCardInExile(opponent, "Forest")
        driver.putCardInExile(you, "Island")

        val (_, log) = driver.flashOverBears(you, opponent, accept = true, pick = emptyList())

        log.askedYesNo shouldBe false
        log.minPick shouldBe null
        driver.state.getZone(opponent, Zone.EXILE) shouldContain only
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }

    test("declining leaves the exiled cards and lets the spell resolve") {
        val (driver, you, opponent) = setup()
        val first = driver.putCardInExile(opponent, "Forest")
        val second = driver.putCardInExile(opponent, "Forest")

        val (_, log) = driver.flashOverBears(you, opponent, accept = false, pick = emptyList())

        log.askedYesNo shouldBe true
        driver.state.getZone(opponent, Zone.EXILE) shouldContainAll listOf(first, second)
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
