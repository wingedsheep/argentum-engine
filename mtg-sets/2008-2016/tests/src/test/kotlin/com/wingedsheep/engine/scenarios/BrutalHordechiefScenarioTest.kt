package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brutal Hordechief (FRF #64) — "Whenever a creature you control attacks, defending player loses 1
 * life and you gain 1 life. {3}{R/W}{R/W}: Creatures your opponents control block this turn if
 * able, and you choose how those creatures block."
 *
 * One drain per attacker; after the ability resolves the defender's block declaration is offered
 * to Hordechief's controller, who must make the defender's creatures block if they can, and may
 * pile them all onto one attacker.
 */
class BrutalHordechiefScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame =
        scenario()
            .withPlayers("Hordechief", "Defender")
            .withCardOnBattlefield(1, "Brutal Hordechief")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Llanowar Elves")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    private fun TestGame.advanceTo(step: Step) {
        var guard = 0
        while (state.step != step && guard++ < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
        }
        state.step shouldBe step
    }

    private fun TestGame.activateHordechief() {
        val hordechief = findPermanent("Brutal Hordechief")!!
        val ability = cardRegistry.getCard("Brutal Hordechief")!!.script.activatedAbilities.single()
        execute(ActivateAbility(player1Id, hordechief, ability.id)).error shouldBe null
        resolveStack()
    }

    init {
        test("each attacker drains the defending player") {
            val game = board()
            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Brutal Hordechief" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("you choose how the opponent's creatures block, and they must block if able") {
            val game = board()
            game.activateHordechief()
            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Brutal Hordechief" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.advanceTo(Step.DECLARE_BLOCKERS)

            withClue("the defender no longer declares their own blocks") {
                game.getLegalActions(2).none { it.action is DeclareBlockers } shouldBe true
            }
            (game.getLegalActions(1).single { it.action is DeclareBlockers }.action as DeclareBlockers)
                .playerId shouldBe game.player2Id
            withClue("keeping every creature home breaks the block requirement") {
                game.execute(DeclareBlockers(game.player2Id, emptyMap())).error shouldNotBe null
            }

            // Both of the defender's creatures pile onto the Bears; Hordechief goes unblocked.
            val bears = game.findPermanent("Grizzly Bears")!!
            val blocks = mapOf(
                game.findPermanent("Hill Giant")!! to listOf(bears),
                game.findPermanent("Llanowar Elves")!! to listOf(bears)
            )
            game.execute(DeclareBlockers(game.player2Id, blocks)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("two drains plus Hordechief's 3 unblocked damage") {
                game.getLifeTotal(2) shouldBe 15
            }
            game.findPermanent("Grizzly Bears") shouldBe null
        }
    }
}
