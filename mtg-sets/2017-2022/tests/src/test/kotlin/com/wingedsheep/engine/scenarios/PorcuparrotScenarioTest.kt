package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.iko.cards.Porcuparrot
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Porcuparrot (IKO #128) — {3}{R} Creature — Bird Beast 3/4.
 *
 *   Mutate {2}{R}
 *   {T}: This creature deals X damage to any target, where X is the number of times this creature
 *   has mutated.
 *
 * Mutated once onto Grizzly Bears the ping deals 1; mutated twice it deals 2. Cast normally it has
 * never mutated, so its tap deals nothing.
 */
class PorcuparrotScenarioTest : ScenarioTestBase() {

    private val pingId = Porcuparrot.activatedAbilities[0].id

    private fun TestGame.mutateOnto(host: com.wingedsheep.sdk.model.EntityId, over: Boolean) {
        val offer = getLegalActions(1).first { (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(host)))).error shouldBe null
        resolveStack()
        answerYesNo(over)
        resolveStack()
    }

    private fun TestGame.pingOpponent(source: com.wingedsheep.sdk.model.EntityId) {
        execute(ActivateAbility(player1Id, source, pingId, targets = listOf(ChosenTarget.Player(player2Id)))).error shouldBe null
        resolveStack()
    }

    init {
        context("Porcuparrot") {

            test("deals damage equal to the number of times it has mutated") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Porcuparrot")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.mutateOnto(bears, over = false)
                withClue("mutated once, under the Bears: the merged Bears pings for 1") {
                    game.pingOpponent(bears)
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("a second mutation raises X to 2") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Porcuparrot")
                    .withCardInHand(1, "Porcuparrot")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.mutateOnto(bears, over = true)
                game.mutateOnto(bears, over = true)
                game.pingOpponent(bears)
                game.getLifeTotal(2) shouldBe 18
            }

            test("a Porcuparrot that never mutated deals no damage") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Porcuparrot")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.pingOpponent(game.findPermanent("Porcuparrot")!!)
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
