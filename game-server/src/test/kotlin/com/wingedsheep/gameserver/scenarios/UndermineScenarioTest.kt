package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Undermine.
 *
 * Undermine ({U}{U}{B}, Instant):
 *   "Counter target spell. Its controller loses 3 life."
 *
 * Exercises the `CounterSpell then LoseLife(EffectTarget.TargetController)` composition —
 * in particular that the life loss is applied to the *countered spell's controller*, not the
 * caster of Undermine.
 */
class UndermineScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Test Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 2, 2)
        )

        context("Undermine effect") {
            test("counters a spell and its controller loses 3 life") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Undermine")
                    .withCardInHand(2, "Test Bear")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Test Bear; Caster responds with Undermine.
                val bearResult = game.castSpell(2, "Test Bear")
                withClue("Test Bear should be cast: ${bearResult.error}") {
                    bearResult.error shouldBe null
                }
                game.execute(PassPriority(game.player2Id))

                val counterResult = game.castSpellTargetingStackSpell(1, "Undermine", "Test Bear")
                withClue("Undermine should be cast: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Test Bear should be countered (in Opponent's graveyard)") {
                    game.isOnBattlefield("Test Bear") shouldBe false
                    game.isInGraveyard(2, "Test Bear") shouldBe true
                }
                withClue("The countered spell's controller (Opponent) loses 3 life: 20 -> 17") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Undermine's caster does not lose life") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
