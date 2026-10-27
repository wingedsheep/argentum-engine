package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shackles of Treachery (KHM #150) — {2}{R} Sorcery.
 *
 *   Gain control of target creature until end of turn. Untap that creature. Until end of turn, it
 *   gains haste and "Whenever this creature deals damage, destroy target Equipment attached to it."
 *
 * The granted trigger's target filter is source-relative (Equipment attached to the stolen
 * creature), so the test proves the trigger's source is the creature and that only Equipment
 * attached to it — regardless of who controls that Equipment — is a legal target.
 */
class ShacklesOfTreacheryScenarioTest : ScenarioTestBase() {
    init {
        context("Shackles of Treachery") {

            test("stolen creature attacks with haste and its damage destroys the opponent's Equipment attached to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shackles of Treachery")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardAttachedTo(2, "Bonesplitter", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Leonin Scimitar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shackles of Treachery", bears).error shouldBe null
                game.resolveStack()

                withClue("Player1 controls the untapped Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
                withClue("gaining control of the creature doesn't steal the Equipment on it") {
                    game.state.projectedState.getController(game.findPermanent("Bonesplitter")!!) shouldBe game.player2Id
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 20) game.passPriority()
                val decision = game.state.pendingDecision as ChooseTargetsDecision
                withClue("only the Equipment attached to the stolen creature is a legal target") {
                    decision.legalTargets[0] shouldBe listOf(game.findPermanent("Bonesplitter")!!)
                }
                game.selectTargets(listOf(game.findPermanent("Bonesplitter")!!)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Bears with Bonesplitter dealt 4") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("the Equipment attached to the stolen creature is destroyed") {
                    game.isInGraveyard(2, "Bonesplitter") shouldBe true
                }
                withClue("an Equipment not attached to it is not a legal target") {
                    game.isOnBattlefield("Leonin Scimitar") shouldBe true
                }
            }

            test("with no Equipment attached the trigger has no target and nothing is destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shackles of Treachery")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shackles of Treachery", bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 18
                game.isOnBattlefield("Bonesplitter") shouldBe true
            }
        }
    }
}
