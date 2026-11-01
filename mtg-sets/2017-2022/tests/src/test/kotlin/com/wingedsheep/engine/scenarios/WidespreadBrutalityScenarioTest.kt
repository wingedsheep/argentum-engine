package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Widespread Brutality (WAR #226) — {1}{B}{R}{R} Sorcery.
 *
 * "Amass Zombies 2, then the Army you amassed deals damage equal to its power to each non-Army
 * creature."
 *
 * Exercises `EffectTarget.AmassedArmy` as both the damage amount and the damage *source* inside a
 * `ForEachInGroup` loop — the pipeline's amassed-Army slot must survive into each iteration, and
 * the Army (black), not the black-red spell, deals the damage.
 */
class WidespreadBrutalityScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Widespread Brutality") {

            test("fresh Army deals 2 to each non-Army creature, sparing itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Widespread Brutality")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Widespread Brutality")
                withClue("Casting Widespread Brutality should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val army = game.findPermanent("Zombie Army")
                    ?: error("Widespread Brutality should have created a Zombie Army")
                withClue("Amass Zombies 2 makes the 0/0 token a 2/2") {
                    projector.project(game.state).getPower(army) shouldBe 2
                }
                withClue("The Army is not a non-Army creature, so it takes no damage") {
                    (game.state.getEntity(army)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
                withClue("Grizzly Bears (2/2) takes 2 and dies") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                val giant = game.findPermanent("Hill Giant") ?: error("Hill Giant should survive 2 damage")
                withClue("The caster's own non-Army creature is hit too") {
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }

            test("an existing Army grows and deals its full power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Widespread Brutality")
                    .withCardInHand(1, "Widespread Brutality")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Widespread Brutality").error shouldBe null
                game.resolveStack()
                val army = game.findPermanent("Zombie Army") ?: error("first cast should create an Army")
                game.state.getEntity(game.findPermanent("Hill Giant")!!)?.get<DamageComponent>()?.amount shouldBe 2

                game.castSpell(1, "Widespread Brutality").error shouldBe null
                game.resolveStack()

                withClue("The second amass reuses the same Army rather than creating another") {
                    game.findPermanents("Zombie Army").size shouldBe 1
                    projector.project(game.state).getPower(army) shouldBe 4
                }
                withClue("Hill Giant takes 4 more (6 total) and dies") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("the Army is the damage source, so protection from red doesn't prevent it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Widespread Brutality")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Voice of Law")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Widespread Brutality").error shouldBe null
                game.resolveStack()

                withClue("Voice of Law (2/2, protection from red) takes 2 from the black Army and dies") {
                    game.isInGraveyard(2, "Voice of Law") shouldBe true
                }
            }
        }
    }
}
