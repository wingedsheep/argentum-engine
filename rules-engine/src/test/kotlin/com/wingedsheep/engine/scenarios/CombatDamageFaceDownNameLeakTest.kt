package com.wingedsheep.engine.scenarios

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
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine-level combat-damage board contract plus a regression for CR 708.2a: a face-down (morph /
 * manifest) permanent has no abilities, so the combat-damage-step pre-checks that key off a card's
 * [AssignCombatDamageAsUnblocked] / [DivideCombatDamageFreely] static abilities must NOT fire for a
 * face-down creature. Those pre-checks read the abilities off the *face-up* [CardDefinition], so
 * firing them for a face-down morph both mis-applies the ability and leaks the hidden card's name
 * into the decision prompt (a [CombatResolutionDecision] would never be reached).
 *
 * The names on the board nodes here are the *real* card names — per-viewer face-down masking is
 * applied downstream at delivery time (game-server `DecisionEnricher`), covered by
 * `CombatDamageMaskingEnricherTest`. This test locks that the engine itself does not mask, so there
 * is a single masking point.
 */
class CombatDamageFaceDownNameLeakTest : FunSpec({

    // A creature whose face-up card carries BOTH combat-damage-assignment static abilities. Morphed,
    // it is a vanilla 2/2, so neither ability may be offered.
    val sneakyBrute = CardDefinition.creature(
        name = "Sneaky Brute",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Ogre")),
        power = 3,
        toughness = 3,
        script = CardScript(
            staticAbilities = listOf(AssignCombatDamageAsUnblocked(), DivideCombatDamageFreely()),
        ),
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(sneakyBrute))
        return driver
    }

    /** Turn an on-battlefield creature into a face-down morph (keeps its hidden real name). */
    fun GameTestDriver.morphFaceDown(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { container ->
            val defId = container.get<CardComponent>()?.cardDefinitionId ?: ""
            container.with(FaceDownComponent).with(MorphDataComponent(PayCost.OwnManaCost, defId))
        })
    }

    /** Turn an on-battlefield creature into a face-down manifested permanent. */
    fun GameTestDriver.manifestFaceDown(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { container ->
            container.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.MANIFEST))
        })
    }

    /** Advance steps until a pending decision shows up (without auto-resolving). */
    fun advanceUntilDecision(driver: GameTestDriver, maxPasses: Int = 50) {
        var passes = 0
        while (driver.state.pendingDecision == null && passes < maxPasses) {
            val priority = driver.state.priorityPlayerId ?: error("No priority and no pending decision")
            driver.submit(PassPriority(priority))
            passes++
            if (driver.state.gameOver) error("Game ended before a decision was emitted")
        }
        if (passes >= maxPasses) {
            error("No pending decision emitted within $maxPasses passes; current step=${driver.currentStep}")
        }
    }

    test("a face-down attacker's face-up combat-damage abilities are not offered (CR 708.2a); board keeps real names") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Face-down morph attacker — really "Sneaky Brute" (assign-as-unblocked + divide-freely),
        // face down it is a vanilla 2/2. If either ability fired, the engine would pause on that
        // ability's own decision (a YesNoDecision / DistributeDecision) instead of the board.
        val morphAttacker = driver.putCreatureOnBattlefield(attacker, "Sneaky Brute")
        driver.morphFaceDown(morphAttacker)
        driver.removeSummoningSickness(morphAttacker)

        // Two blockers so the 2/2 attacker must assign combat damage (CR 510.1c) → board decision.
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

        // Declaring two blockers on one attacker pauses on an OrderObjectsDecision first.
        var decision: PendingDecision? = driver.state.pendingDecision
        if (decision is OrderObjectsDecision) {
            driver.submitDecision(
                decision.playerId,
                OrderedResponse(decision.id, listOf(manifestBlocker, plainBlocker)),
            )
        }
        advanceUntilDecision(driver)
        decision = driver.state.pendingDecision

        // Reaching the board (rather than an ability decision) proves both CR 708.2a guards fired.
        decision.shouldBeInstanceOf<CombatResolutionDecision>()

        // The engine keeps the real names; masking is per-viewer downstream.
        decision.attackers.single { it.id == morphAttacker }.name shouldBe "Sneaky Brute"
        decision.blockers.single { it.id == manifestBlocker }.name shouldBe "Savannah Lions"
        decision.blockers.single { it.id == plainBlocker }.name shouldBe "Trample Beast"
    }
})
