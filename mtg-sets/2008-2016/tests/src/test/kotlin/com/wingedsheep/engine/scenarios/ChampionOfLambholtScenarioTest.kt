package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Champion of Lambholt (AVR #171).
 *
 * "Creatures with power less than this creature's power can't block creatures you control. /
 *  Whenever another creature you control enters, put a +1/+1 counter on this creature."
 *
 * The group-scoped `CantBeBlockedByCreaturesWithLessPower` compares each would-be blocker against
 * Champion's own projected power — not the attacker's — and covers every creature its controller
 * controls, Champion included. Each creature entering grows Champion and so raises the bar.
 */
class ChampionOfLambholtScenarioTest : ScenarioTestBase() {

    private val champion = "Champion of Lambholt"

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, champion, summoningSickness = false)
        .withCardOnBattlefield(1, "Runeclaw Bear", summoningSickness = false)
        .withCardInHand(1, "Llanowar Elves")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(2, "Raging Goblin", summoningSickness = false)
        .withCardOnBattlefield(2, "Gray Ogre", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackWithBoth() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf(champion to 2, "Runeclaw Bear" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
    }

    init {
        test("a 1/1 Champion sets the bar at 1: a 1-power creature can still block the Bear") {
            val game = board()
            game.attackWithBoth()
            withClue("power 1 is not less than Champion's power 1") {
                game.declareBlockers(mapOf("Raging Goblin" to listOf("Runeclaw Bear"))).error shouldBe null
            }
        }

        test("another creature entering grows Champion; smaller creatures can't block anything you control") {
            val game = board()
            game.castSpell(1, "Llanowar Elves").error shouldBe null
            game.resolveStack()
            val power = game.state.projectedState.getPower(game.findPermanent(champion)!!)
            withClue("the Elves entering put a +1/+1 counter on Champion") { power shouldBe 2 }

            game.attackWithBoth()
            withClue("Raging Goblin's power 1 is less than Champion's 2: it can't block the Bear") {
                game.declareBlockers(mapOf("Raging Goblin" to listOf("Runeclaw Bear"))).error shouldNotBe null
            }
            withClue("... nor Champion itself") {
                game.declareBlockers(mapOf("Raging Goblin" to listOf(champion))).error shouldNotBe null
            }
            withClue("Gray Ogre's power 2 is not less than 2: it can block") {
                game.declareBlockers(mapOf("Gray Ogre" to listOf("Runeclaw Bear"))).error shouldBe null
            }
        }
    }
}
