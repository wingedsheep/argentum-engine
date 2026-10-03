package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Collective Resistance (MH3 #147) — {1}{G} instant, Escalate {G}, three modes.
 * Pins the escalate mana scaling and each mode's resolution.
 */
class CollectiveResistanceScenarioTest : ScenarioTestBase() {

    init {
        context("Collective Resistance") {

            fun ScenarioTestBase.TestGame.castResistance(
                modes: List<Int>,
                modeTargets: List<List<ChosenTarget>>,
            ) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first {
                        state.getEntity(it)?.get<CardComponent>()?.name == "Collective Resistance"
                    },
                    targets = modeTargets.flatten(),
                    chosenModes = modes,
                    modeTargetsOrdered = modeTargets,
                )
            )

            fun board(forests: Int) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Collective Resistance")
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", forests)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("one mode costs {1}{G} and destroys the target artifact") {
                val game = board(2)
                val thopter = game.findPermanent("Ornithopter")!!
                val cast = game.castResistance(listOf(0), listOf(listOf(ChosenTarget.Permanent(thopter))))
                withClue("single mode pays no escalate: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.findPermanent("Glorious Anthem") shouldNotBe null
            }

            test("all three modes cost {1}{G}{G}{G} and every mode resolves") {
                val game = board(4)
                val thopter = game.findPermanent("Ornithopter")!!
                val anthem = game.findPermanent("Glorious Anthem")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castResistance(
                    listOf(0, 1, 2),
                    listOf(
                        listOf(ChosenTarget.Permanent(thopter)),
                        listOf(ChosenTarget.Permanent(anthem)),
                        listOf(ChosenTarget.Permanent(bears)),
                    ),
                )
                withClue("four lands pay base + two escalates: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.isInGraveyard(2, "Glorious Anthem") shouldBe true
                val projected = game.state.projectedState
                projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
                projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            test("three modes are unpayable with only three lands") {
                val game = board(3)
                val thopter = game.findPermanent("Ornithopter")!!
                val anthem = game.findPermanent("Glorious Anthem")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castResistance(
                    listOf(0, 1, 2),
                    listOf(
                        listOf(ChosenTarget.Permanent(thopter)),
                        listOf(ChosenTarget.Permanent(anthem)),
                        listOf(ChosenTarget.Permanent(bears)),
                    ),
                )
                withClue("the second escalate {G} can't be paid") { cast.error shouldNotBe null }
                game.findPermanent("Ornithopter") shouldNotBe null
            }
        }
    }
}
