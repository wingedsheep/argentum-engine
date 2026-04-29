package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bristlebane Outrider.
 *
 * Bristlebane Outrider
 * {3}{G}
 * Creature — Kithkin Knight
 * 3/5
 *
 * This creature can't be blocked by creatures with power 2 or less.
 * As long as another creature entered the battlefield under your control this turn,
 * this creature gets +2/+0.
 */
class BristlebaneOutriderScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val vanillaBear = CardDefinition.creature(
        name = "Vanilla Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )

    init {
        cardRegistry.register(vanillaBear)

        test("base stats with no other creature entered this turn — no buff") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bristlebane Outrider")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val outrider = game.findPermanent("Bristlebane Outrider")
            outrider.shouldNotBeNull()

            withClue("Outrider should be base 3/5 when no other creature has entered this turn") {
                projector.getProjectedPower(game.state, outrider) shouldBe 3
                projector.getProjectedToughness(game.state, outrider) shouldBe 5
            }
        }

        test("buff applies when another creature enters the battlefield this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bristlebane Outrider")
                .withCardInHand(1, "Vanilla Bear")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val outrider = game.findPermanent("Bristlebane Outrider")
            outrider.shouldNotBeNull()

            // Cast Vanilla Bear so it enters the battlefield this turn
            game.castSpell(1, "Vanilla Bear")
            game.resolveStack()

            withClue("Outrider should be 5/5 (+2/+0) once another creature has entered this turn") {
                projector.getProjectedPower(game.state, outrider) shouldBe 5
                projector.getProjectedToughness(game.state, outrider) shouldBe 5
            }
        }

        test("Outrider entering on its own does not buff itself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bristlebane Outrider")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            // Cast the Outrider — it enters this turn, but nothing else has.
            game.castSpell(1, "Bristlebane Outrider")
            game.resolveStack()

            val outrider = game.findPermanent("Bristlebane Outrider")
            outrider.shouldNotBeNull()

            withClue("Outrider should not buff itself just because it entered this turn") {
                projector.getProjectedPower(game.state, outrider) shouldBe 3
                projector.getProjectedToughness(game.state, outrider) shouldBe 5
            }
        }

        test("creature entering under opponent's control does not trigger buff") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bristlebane Outrider")
                .withCardInHand(2, "Vanilla Bear")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(2)
                .build()

            game.castSpell(2, "Vanilla Bear")
            game.resolveStack()

            val outrider = game.findPermanent("Bristlebane Outrider")
            outrider.shouldNotBeNull()

            withClue("Opponent's creature entering should not buff Outrider") {
                projector.getProjectedPower(game.state, outrider) shouldBe 3
                projector.getProjectedToughness(game.state, outrider) shouldBe 5
            }
        }
    }
}
