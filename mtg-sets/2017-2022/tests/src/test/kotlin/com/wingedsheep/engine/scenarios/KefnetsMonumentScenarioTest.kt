package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kefnet's Monument (AKH #231) — {3} Legendary Artifact.
 *
 *   Blue creature spells you cast cost {1} less to cast.
 *   Whenever you cast a creature spell, target creature an opponent controls doesn't untap during
 *   its controller's next untap step.
 *
 * The duration's anchor (the *affected* controller's next untap step) is proven by
 * BarlsCageScenarioTest; here: only blue creatures are cheaper, but any creature spell triggers.
 */
class KefnetsMonumentScenarioTest : ScenarioTestBase() {

    init {
        context("Kefnet's Monument") {

            test("a blue creature spell costs {1} less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kefnet's Monument")
                    .withCardInHand(1, "Coral Merfolk") // {1}{U}
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Coral Merfolk").error shouldBe null
            }

            test("a non-blue creature spell is not cheaper") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kefnet's Monument")
                    .withCardInHand(1, "Grizzly Bears") // {1}{G}
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            }

            test("any creature spell locks down target creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kefnet's Monument")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's creature doesn't untap during its next untap step") {
                    game.state.projectedState.hasKeyword(giant, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
