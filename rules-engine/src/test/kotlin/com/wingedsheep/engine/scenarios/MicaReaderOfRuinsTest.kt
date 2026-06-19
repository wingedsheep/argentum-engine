package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.MicaReaderOfRuins
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Mica, Reader of Ruins {3}{R} — Human Artificer 4/4.
 * "Ward—Pay 3 life.
 *  Whenever you cast an instant or sorcery spell, you may sacrifice an artifact. If you do,
 *  copy that spell and you may choose new targets for the copy."
 *
 * These tests pin the load-bearing composition: the instant/sorcery cast trigger and the optional
 * sacrifice gate (no sacrifice → no copy; sacrifice → a copy of the triggering spell resolves).
 */
class MicaReaderOfRuinsTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MicaReaderOfRuins))
        return driver
    }

    /** Put Mica on the battlefield for the active player; give an artifact to sacrifice. */
    fun GameTestDriver.setup(): Triple<EntityId, EntityId, EntityId> {
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = activePlayer!!
        val opponent = getOpponent(you)
        putCreatureOnBattlefield(you, "Mica, Reader of Ruins")
        val artifact = putPermanentOnBattlefield(you, "Artifact Creature")
        return Triple(you, opponent, artifact)
    }

    /**
     * Resolve the whole stack after the instant/sorcery is cast, optionally accepting Mica's
     * sacrifice gate. Handles three decision shapes: the may-pay yes/no gate, the artifact
     * selection, and the copy's "choose new targets" (keep the same target — the opponent).
     */
    fun GameTestDriver.resolveMicaTrigger(
        you: EntityId,
        opponent: EntityId,
        artifact: EntityId,
        acceptSacrifice: Boolean
    ) {
        var answeredGate = false
        var guard = 0
        while (stackSize > 0 && guard++ < 40) {
            val decision = pendingDecision
            when {
                decision == null -> bothPass()
                decision is YesNoDecision && !answeredGate -> {
                    answeredGate = true
                    submitYesNo(you, acceptSacrifice)
                }
                decision is SelectCardsDecision -> submitCardSelection(you, listOf(artifact))
                decision is ChooseTargetsDecision -> submitTargetSelection(you, listOf(opponent))
                else -> autoResolveDecision()
            }
        }
    }

    test("declining the sacrifice makes no copy — opponent takes only the original bolt") {
        val driver = createDriver()
        val (you, opponent, artifact) = driver.setup()
        val oppLifeBefore = driver.getLifeTotal(opponent)

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null

        driver.resolveMicaTrigger(you, opponent, artifact, acceptSacrifice = false)

        // Only the original Lightning Bolt (3) resolves.
        driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 3)
        // Artifact survives (still on the battlefield).
        (driver.state.getEntity(artifact) != null).shouldBeTrue()
    }

    test("sacrificing an artifact copies the spell — opponent takes the bolt plus its copy") {
        val driver = createDriver()
        val (you, opponent, artifact) = driver.setup()
        val oppLifeBefore = driver.getLifeTotal(opponent)

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null

        driver.resolveMicaTrigger(you, opponent, artifact, acceptSacrifice = true)

        // Original Lightning Bolt (3) + the copy (3) = 6 damage.
        driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 6)
        // The sacrificed artifact is gone.
        (driver.state.getEntity(artifact) == null ||
            driver.state.getBattlefield().none { it == artifact }).shouldBeTrue()
    }
})
