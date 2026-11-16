package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirror Mockery (DTK #62) — {1}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   Whenever enchanted creature attacks, you may create a token that's a copy of that creature.
 *   Exile that token at end of combat.
 *
 * Pins what the script alone can't show: the copy goes to the Aura's controller even when the
 * enchanted creature is an opponent's (and that player makes the "may" choice), the token isn't
 * attacking, and the delayed exile fires at end of combat — not at the end step.
 */
class MirrorMockeryScenarioTest : ScenarioTestBase() {

    init {
        context("Mirror Mockery") {

            test("the copy enters untapped and not attacking, and is exiled at end of combat") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Mirror Mockery", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger asks Alice whether to make the copy") {
                    game.state.pendingDecision!!.playerId shouldBe game.player1Id
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Grizzly Bears").single { it != bears }
                withClue("the token is Alice's and isn't attacking (first ruling)") {
                    game.state.projectedState.getController(token) shouldBe game.player1Id
                    game.state.getEntity(token)?.has<AttackingComponent>() shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the token is exiled at end of combat; the original stays") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                }
            }

            test("enchanting an opponent's attacker gives the Aura's controller the copy") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Mirror Mockery", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()

                withClue("\"you\" is the Aura's controller, so Alice decides") {
                    game.state.pendingDecision!!.playerId shouldBe game.player1Id
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val token = game.findPermanents("Grizzly Bears").single { it != bears }
                game.state.projectedState.getController(token) shouldBe game.player1Id
            }

            test("declining creates no token") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Mirror Mockery", "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findPermanents("Grizzly Bears").size shouldBe 1
            }
        }
    }
}
