package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Per-viewer masking of the combat-damage board (state masking is a game-server concern). The
 * engine emits ONE [CombatResolutionDecision] with real card names, shown to both choosers (the
 * attacker assigns its damage, the defender assigns any blocker damage). [DecisionEnricher] masks a
 * face-down (morph / manifest) creature's real name for every viewer who doesn't control it — the
 * controller keeps its own creature's name, mirroring the battlefield masking in ClientStateTransformer.
 */
class CombatDamageMaskingEnricherTest : FunSpec({

    fun GameTestDriver.morphFaceDown(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { container ->
            val defId = container.get<CardComponent>()?.cardDefinitionId ?: ""
            container.with(FaceDownComponent).with(MorphDataComponent(PayCost.OwnManaCost, defId))
        })
    }

    fun GameTestDriver.manifestFaceDown(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { container ->
            container.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.MANIFEST))
        })
    }

    fun advanceUntilDecision(driver: GameTestDriver, maxPasses: Int = 50) {
        var passes = 0
        while (driver.state.pendingDecision == null && passes < maxPasses) {
            val priority = driver.state.priorityPlayerId ?: error("No priority and no pending decision")
            driver.submit(PassPriority(priority))
            passes++
            if (driver.state.gameOver) error("Game ended before a decision was emitted")
        }
        if (passes >= maxPasses) error("No pending decision within $maxPasses passes; step=${driver.currentStep}")
    }

    test("combat-damage board masks face-down names per viewer") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Face-down morph attacker (really "Centaur Courser"), controlled by the attacker.
        val morphAttacker = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.morphFaceDown(morphAttacker)
        driver.removeSummoningSickness(morphAttacker)

        // Two blockers so the 2/2 attacker must assign combat damage → board decision. One is a
        // face-down manifest (really "Savannah Lions") the DEFENDER controls, one is plain.
        val manifestBlocker = driver.putCreatureOnBattlefield(defender, "Savannah Lions")
        driver.manifestFaceDown(manifestBlocker)
        val plainBlocker = driver.putCreatureOnBattlefield(defender, "Trample Beast")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(morphAttacker), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(
            defender,
            mapOf(manifestBlocker to listOf(morphAttacker), plainBlocker to listOf(morphAttacker)),
        )

        val ordering = driver.state.pendingDecision
        if (ordering is OrderObjectsDecision) {
            driver.submitDecision(ordering.playerId, OrderedResponse(ordering.id, listOf(manifestBlocker, plainBlocker)))
        }
        advanceUntilDecision(driver)
        val decision = driver.state.pendingDecision
        decision.shouldBeInstanceOf<CombatResolutionDecision>()

        // Engine keeps the real names; masking happens in the enricher, per viewer.
        decision.attackers.single { it.id == morphAttacker }.name shouldBe "Centaur Courser"

        val enricher = DecisionEnricher(driver.cardRegistry)

        // The attacker's controller sees their OWN morph's real name; the opponent's manifest is masked.
        val forAttacker = enricher.enrich(decision, driver.state, attacker) as CombatResolutionDecision
        forAttacker.attackers.single { it.id == morphAttacker }.name shouldBe "Centaur Courser"
        forAttacker.blockers.single { it.id == manifestBlocker }.name shouldBe "Face-down creature"
        forAttacker.blockers.single { it.id == plainBlocker }.name shouldBe "Trample Beast"
        forAttacker.prompt shouldContain "Centaur Courser"

        // The defender sees the attacker's morph masked on the board node, the prompt, and the source
        // name — but keeps the real name of the manifest creature THEY control.
        val forDefender = enricher.enrich(decision, driver.state, defender) as CombatResolutionDecision
        forDefender.attackers.single { it.id == morphAttacker }.name shouldBe "Face-down creature"
        forDefender.blockers.single { it.id == manifestBlocker }.name shouldBe "Savannah Lions"
        forDefender.prompt shouldNotContain "Centaur Courser"
        forDefender.context.sourceName shouldBe "Face-down creature"

        // The opponent-decision status shown to the defender also masks the attacker's real name.
        val status = enricher.createOpponentDecisionStatus(decision, driver.state, defender)
        (status.sourceName ?: "") shouldNotContain "Centaur Courser"
    }
})
