package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Packsong Pup (VOW #213) — {1}{G} Creature — Wolf, 1/1.
 *
 *   At the beginning of combat on your turn, if you control another Wolf or Werewolf, put a
 *   +1/+1 counter on this creature.
 *   When this creature dies, you gain life equal to its power.
 *
 * Exercises both halves: the conditional begin-combat counter (only fires while controlling
 * another Wolf/Werewolf) and the dies trigger paying out life equal to Packsong Pup's power at
 * the time it died (including any +1/+1 counters already on it).
 */
class PacksongPupScenarioTest : ScenarioTestBase() {

    init {
        context("Packsong Pup — conditional begin-combat counter") {

            test("without another Wolf or Werewolf, no counter is added at begin of combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Packsong Pup", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pup = game.findPermanent("Packsong Pup")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("no other Wolf/Werewolf: no +1/+1 counter is added") {
                    val counters = game.state.getEntity(pup)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 0
                }
            }

            test("with another Wolf you control, a +1/+1 counter is added at begin of combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Packsong Pup", summoningSickness = false)
                    .withCardOnBattlefield(1, "Lightning Wolf", summoningSickness = false) // another Wolf
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pup = game.findPermanent("Packsong Pup")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("controlling another Wolf adds a +1/+1 counter") {
                    val counters = game.state.getEntity(pup)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 1
                }
            }
        }

        context("Packsong Pup — dies trigger gains life equal to its power") {

            test("dying gains its controller life equal to its power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Packsong Pup", summoningSickness = false)
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pup = game.findPermanent("Packsong Pup")!!

                game.castSpell(2, "Doom Blade", pup).error shouldBe null
                game.resolveStack()

                withClue("Packsong Pup died to Doom Blade") {
                    game.isOnBattlefield("Packsong Pup") shouldBe false
                }
                withClue("its controller gains 1 life (its power) from the dies trigger") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }
        }
    }
}
