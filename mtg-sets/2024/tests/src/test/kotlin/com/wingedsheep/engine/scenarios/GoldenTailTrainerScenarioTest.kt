package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Golden-Tail Trainer (MH3 #187) — {1}{G}{W} 1/3 Fox Samurai.
 *
 * "Aura and Equipment spells you cast cost {X} less to cast, where X is this creature's power.
 *  Whenever this creature attacks, other modified creatures you control get +X/+X until end of
 *  turn, where X is this creature's power."
 */
class GoldenTailTrainerScenarioTest : ScenarioTestBase() {

    private fun TestGame.addPlusOneCounter(id: EntityId) {
        state = state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        }
    }

    init {
        context("Golden-Tail Trainer") {

            test("Equipment costs {X} less where X is the Trainer's power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden-Tail Trainer")
                    .withCardInHand(1, "Vulshok Morningstar")
                    .withActivePlayer(1)
                    .build()

                withClue("power 1 only discounts {1} of Vulshok Morningstar's {2}, and there are no lands") {
                    game.castSpell(1, "Vulshok Morningstar").error shouldNotBe null
                }

                game.addPlusOneCounter(game.findPermanent("Golden-Tail Trainer")!!)
                withClue("power 2 makes the {2} Equipment free") {
                    game.castSpell(1, "Vulshok Morningstar").error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Vulshok Morningstar") shouldBe true
            }

            test("Aura spells are discounted on their generic part") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden-Tail Trainer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Pacifism's {1}{W} costs {W} with a power-1 Trainer") {
                    game.castSpell(1, "Pacifism", bears).error shouldBe null
                }
            }

            test("attacking pumps other modified creatures by the Trainer's power, not unmodified ones") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Golden-Tail Trainer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardAttachedTo(1, "Holy Strength", "Savannah Lions")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val trainer = game.findPermanent("Golden-Tail Trainer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.addPlusOneCounter(trainer) // Trainer is now 2/4
                game.addPlusOneCounter(bears) // Bears 3/3, modified by a counter

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Golden-Tail Trainer" to 2)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Bears (counter) get +2/+2: 3/3 -> 5/5") {
                    projected.getPower(bears) shouldBe 5
                    projected.getToughness(bears) shouldBe 5
                }
                withClue("Lions (your Aura) get +2/+2: 1/1 test Lions with Holy Strength 2/3 -> 4/5") {
                    projected.getPower(lions) shouldBe 4
                    projected.getToughness(lions) shouldBe 5
                }
                withClue("unmodified Hill Giant stays 3/3") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                }
                withClue("the Trainer itself is excluded ('other') and stays 2/4") {
                    projected.getPower(trainer) shouldBe 2
                    projected.getToughness(trainer) shouldBe 4
                }
            }
        }
    }
}
