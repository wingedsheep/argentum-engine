package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Galion, Elvenking's Butler (HOB) — "Whenever Galion attacks, choose up to one other target
 * creature you control. Its base power and toughness become equal to Galion's power and toughness
 * until end of turn."
 *
 * Two things worth pinning: the dynamic amounts read *Galion*, not the creature being changed (a
 * source-vs-affected-entity mix-up would silently make the target copy its own stats and look like a
 * no-op), and "up to one" means attacking with no other creature out is still legal.
 */
class GalionElvenkingsButlerScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    init {
        test("the chosen creature's base power and toughness become Galion's 4/4") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Galion, Elvenking's Butler", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Galion, Elvenking's Butler" to 2)).error shouldBe null

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.selectTargets(listOf(bears))
            game.resolveStack()

            withClue("Grizzly Bears is printed 2/2 and should now be Galion's 4/4") {
                power(game, bears) shouldBe 4
                toughness(game, bears) shouldBe 4
            }
        }

        test("attacking with nothing else on board is legal — the target is 'up to one'") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Galion, Elvenking's Butler", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Galion, Elvenking's Butler" to 2)).error shouldBe null
            game.resolveStack()

            val galion = game.findPermanent("Galion, Elvenking's Butler").shouldNotBeNull()
            power(game, galion) shouldBe 4
        }
    }
}
