package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BrokenDamScenarioTest : ScenarioTestBase() {
    private fun tapped(game: TestGame, name: String) =
        game.state.getEntity(game.findPermanent(name)!!)?.has<TappedComponent>() == true

    private fun cast(game: TestGame, vararg names: String): com.wingedsheep.engine.core.ExecutionResult {
        val cardId = game.findCardsInHand(1, "Broken Dam").single()
        return game.execute(
            CastSpell(game.player1Id, cardId, names.map { ChosenTarget.Permanent(game.findPermanent(it)!!) })
        )
    }

    init {
        context("Broken Dam") {
            test("taps one target creature") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardInHand(1, "Broken Dam")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                cast(game, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                tapped(game, "Grizzly Bears") shouldBe true
                tapped(game, "Hill Giant") shouldBe false
            }

            test("taps two target creatures") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardInHand(1, "Broken Dam")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                cast(game, "Grizzly Bears", "Hill Giant").error shouldBe null
                game.resolveStack()
                tapped(game, "Grizzly Bears") shouldBe true
                tapped(game, "Hill Giant") shouldBe true
            }

            test("cannot target a creature with horsemanship") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardInHand(1, "Broken Dam")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Shu Cavalry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                cast(game, "Shu Cavalry").error shouldNotBe null
            }
        }
    }
}
