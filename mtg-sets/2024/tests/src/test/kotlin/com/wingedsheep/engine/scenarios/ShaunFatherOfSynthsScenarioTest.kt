package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Shaun, Father of Synths {3}{U}{R} — Legendary Creature — Human Scientist 3/4
 *   Whenever you attack, you may create a tapped and attacking token that's a copy of target
 *   attacking legendary creature you control other than Shaun, except it's not legendary and it's
 *   a Synth artifact creature in addition to its other types.
 *   When Shaun leaves the battlefield, exile all Synth tokens you control.
 *
 * Ruling 2024-03-08: the token's controller picks what it attacks as it enters — it needn't be
 * what Shaun attacks (CR 508.4, `EnteringAttackDefender.CONTROLLER_CHOOSES`).
 */
class ShaunFatherOfSynthsScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Shaun, Father of Synths")
        .withCardOnBattlefield(1, "Isamaru, Hound of Konda")
        .withCardOnBattlefield(2, "Jace Beleren")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.synthTokens(): List<EntityId> =
        findAllPermanents("Isamaru, Hound of Konda").filter { state.getEntity(it)!!.has<TokenComponent>() }

    /** Resolve the attack trigger: [create] answers the may, [defender] is the copy's attack. */
    private fun TestGame.resolveTrigger(create: Boolean, defender: EntityId) {
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || state.pendingDecision != null)) {
            when (val decision = state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> submitDecision(YesNoResponse(decision.id, create)).error shouldBe null
                is ChooseTargetsDecision -> {
                    // The "you may" is answered on the target prompt: declining picks no target.
                    val pick = if (defender in decision.legalTargets[0].orEmpty()) listOf(defender)
                    else if (create) listOf(findPermanents("Isamaru, Hound of Konda").first())
                    else emptyList()
                    submitDecision(TargetsResponse(decision.id, mapOf(0 to pick))).error shouldBe null
                }
                else -> error("unexpected decision $decision")
            }
        }
    }

    init {
        test("the copy is a tapped, attacking, non-legendary Synth artifact creature attacking the chosen planeswalker") {
            val game = board()
            val jace = game.findPermanent("Jace Beleren")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shaun, Father of Synths" to 2, "Isamaru, Hound of Konda" to 2)).error shouldBe null
            game.resolveTrigger(create = true, defender = jace)

            val token = game.synthTokens().single()
            val projected = game.state.projectedState
            withClue("not legendary, so the legend rule leaves both Isamarus") {
                projected.isLegendary(token) shouldBe false
                game.findAllPermanents("Isamaru, Hound of Konda") shouldHaveSize 2
            }
            projected.hasType(token, "ARTIFACT") shouldBe true
            projected.isCreature(token) shouldBe true
            projected.hasSubtype(token, "Synth") shouldBe true
            projected.hasSubtype(token, "Dog") shouldBe true
            game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
            withClue("it attacks the planeswalker its controller chose, not Shaun's defender") {
                game.state.getEntity(token)!!.get<AttackingComponent>()!!.defenderId shouldBe jace
            }
        }

        test("declining the may creates no token") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shaun, Father of Synths" to 2, "Isamaru, Hound of Konda" to 2)).error shouldBe null
            game.resolveTrigger(create = false, defender = game.player2Id)

            game.synthTokens() shouldHaveSize 0
        }

        test("Shaun alone can't copy itself: nothing to target, no token") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shaun, Father of Synths" to 2)).error shouldBe null
            game.resolveTrigger(create = true, defender = game.player2Id)

            game.findAllPermanents("Shaun, Father of Synths") shouldHaveSize 1
            game.synthTokens() shouldHaveSize 0
        }

        test("when Shaun leaves the battlefield, its Synth tokens are exiled and the original stays") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shaun, Father of Synths" to 2, "Isamaru, Hound of Konda" to 2)).error shouldBe null
            game.resolveTrigger(create = true, defender = game.player2Id)
            game.synthTokens() shouldHaveSize 1

            game.castSpell(1, "Unsummon", game.findPermanent("Shaun, Father of Synths")).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.findAllPermanents("Shaun, Father of Synths") shouldHaveSize 0
            game.synthTokens() shouldHaveSize 0
            game.findAllPermanents("Isamaru, Hound of Konda").map {
                game.state.getEntity(it)!!.has<TokenComponent>()
            } shouldContainExactlyInAnyOrder listOf(false)
        }
    }
}
