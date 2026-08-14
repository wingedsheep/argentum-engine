package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nighthowl Pursuer (HOB) — {B} Creature — Wolf 1/1.
 *
 * "Menace
 *  Ferocious — Whenever this creature attacks while you control a creature with power 4 or
 *  greater, this creature gets +2/+2 until end of turn."
 *
 * Menace is exercised as a real blocking restriction (one blocker illegal, two legal), and the
 * ferocious pump is checked on both sides of its condition.
 */
class NighthowlPursuerScenarioTest : ScenarioTestBase() {

    init {
        context("Nighthowl Pursuer") {

            test("it has menace") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nighthowl Pursuer")
                    .build()

                val pursuer = game.findPermanent("Nighthowl Pursuer")!!
                game.state.projectedState.hasKeyword(pursuer, Keyword.MENACE) shouldBe true
            }

            test("menace forbids a single blocker but allows two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nighthowl Pursuer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Nighthowl Pursuer" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("one blocker is not enough against menace") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Nighthowl Pursuer")))
                        .error shouldNotBe null
                }
                val two = game.declareBlockers(
                    mapOf(
                        "Grizzly Bears" to listOf("Nighthowl Pursuer"),
                        "Centaur Courser" to listOf("Nighthowl Pursuer")
                    )
                )
                withClue("two blockers satisfy menace: ${two.error}") { two.error shouldBe null }
            }

            test("attacking with a power-4 creature on your side pumps it to 3/3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nighthowl Pursuer")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val pursuer = game.findPermanent("Nighthowl Pursuer")!!
                game.state.projectedState.getPower(pursuer) shouldBe 1

                game.declareAttackers(mapOf("Nighthowl Pursuer" to 2)).error shouldBe null
                game.resolveStack()

                withClue("ferocious granted +2/+2") {
                    game.state.projectedState.getPower(pursuer) shouldBe 3
                    game.state.projectedState.getToughness(pursuer) shouldBe 3
                }
            }

            test("attacking without a power-4 creature leaves it a 1/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nighthowl Pursuer")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val pursuer = game.findPermanent("Nighthowl Pursuer")!!
                game.declareAttackers(mapOf("Nighthowl Pursuer" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the condition is unmet, so no pump") {
                    game.state.projectedState.getPower(pursuer) shouldBe 1
                    game.state.projectedState.getToughness(pursuer) shouldBe 1
                }
            }
        }
    }
}
