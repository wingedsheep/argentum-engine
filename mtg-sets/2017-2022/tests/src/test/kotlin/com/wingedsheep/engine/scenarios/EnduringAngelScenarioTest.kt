package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Enduring Angel // Angelic Enforcer — "If your life total would be reduced to 0 or less, instead
 * transform this creature and your life total becomes 3." The back face is as big as your life
 * total and doubles it whenever it attacks.
 */
class EnduringAngelScenarioTest : ScenarioTestBase() {

    /** The face the angel shows: exactly one of the two is on the battlefield. */
    private fun TestGame.angelName(): String =
        listOf("Enduring Angel", "Angelic Enforcer").single { findPermanent(it) != null }

    init {
        context("Enduring Angel") {
            test("lethal damage transforms it and sets its controller's life to 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe false
                game.getLifeTotal(1) shouldBe 3
                game.angelName() shouldBe "Angelic Enforcer"
                withClue("Angelic Enforcer's power and toughness equal your life total") {
                    val enforcer = game.findPermanent("Angelic Enforcer")!!
                    game.state.projectedState.getPower(enforcer) shouldBe 3
                    game.state.projectedState.getToughness(enforcer) shouldBe 3
                }
            }

            test("Angelic Enforcer doesn't carry the replacement: the next lethal damage ends the game") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.angelName() shouldBe "Angelic Enforcer"

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player2Id
            }

            test("damage that leaves life above 0 is dealt normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 7
                game.angelName() shouldBe "Enduring Angel"
            }

            test("an opponent's lethal damage isn't replaced") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }

            test("simultaneous lethal combat damage transforms it once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(1, 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.state.gameOver shouldBe false
                game.getLifeTotal(1) shouldBe 3
                withClue("a second application would have flipped it back") {
                    game.angelName() shouldBe "Angelic Enforcer"
                }
            }
        }

        context("Enduring Angel dealt lethal damage in the same combat") {
            test("the replacement applies before state-based actions, so it still transforms") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLifeTotal(1, 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 1, "Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Enduring Angel" to listOf("Craw Wurm"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the Bears' damage was replaced while the Angel was still on the battlefield") {
                    game.state.gameOver shouldBe false
                    game.getLifeTotal(1) shouldBe 3
                }
            }
        }

        context("Angelic Enforcer") {
            test("attacking doubles your life total, and its size follows") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Enduring Angel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 3

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Angelic Enforcer" to 2)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 6
                game.state.projectedState.getPower(game.findPermanent("Angelic Enforcer")!!) shouldBe 6
            }
        }
    }
}
