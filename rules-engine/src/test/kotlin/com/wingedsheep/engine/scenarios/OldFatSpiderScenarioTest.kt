package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Old Fat Spider (HOB) — {4}{G}{G} Creature — Spider 6/7.
 *
 * "Reach
 *  This creature can't be blocked by creatures with power 2 or less.
 *  Whenever this creature becomes the target of a spell or ability an opponent controls, draw a card."
 *
 * The blocking restriction is checked on both sides of its boundary (power 2 blocked out, power 3
 * allowed in), and the targeting trigger is checked to fire for an opponent's spell but not for
 * your own.
 */
class OldFatSpiderScenarioTest : ScenarioTestBase() {

    init {
        context("Old Fat Spider") {

            test("it is a 6/7 with reach") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Fat Spider")
                    .build()

                val spider = game.findPermanent("Old Fat Spider")!!
                game.state.projectedState.getPower(spider) shouldBe 6
                game.state.projectedState.getToughness(spider) shouldBe 7
                game.state.projectedState.hasKeyword(spider, Keyword.REACH) shouldBe true
            }

            test("a power-2 creature cannot block it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Fat Spider")
                    // Grizzly Bears is a 2/2 — power 2 is inside the restriction.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Old Fat Spider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("'power 2 or less' includes exactly 2") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Old Fat Spider")))
                        .error shouldNotBe null
                }
            }

            test("a power-3 creature can block it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Fat Spider")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Old Fat Spider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Centaur Courser" to listOf("Old Fat Spider")))
                withClue("power 3 is outside the restriction: ${block.error}") {
                    block.error shouldBe null
                }
            }

            test("an opponent targeting it draws its controller a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Old Fat Spider")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Old Fat Spider")!!
                val libraryBefore = game.librarySize(2)

                game.castSpell(1, "Lightning Bolt", targetId = spider).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Spider's controller drew a card off the targeting trigger") {
                    game.librarySize(2) shouldBe libraryBefore - 1
                    game.handSize(2) shouldBe 1
                }
                withClue("3 damage does not kill a 6/7") {
                    game.isOnBattlefield("Old Fat Spider") shouldBe true
                }
            }

            test("targeting it with your own spell does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Fat Spider")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Old Fat Spider")!!
                val libraryBefore = game.librarySize(1)

                game.castSpell(1, "Giant Growth", targetId = spider).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger reads 'a spell or ability an opponent controls'") {
                    game.librarySize(1) shouldBe libraryBefore
                    game.handSize(1) shouldBe 0
                }
            }
        }
    }
}
