package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skyrider Elf (BFZ #220) — {X}{G}{U} Creature — Elf Warrior Ally 0/0.
 *
 *   Flying
 *   Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it.
 *
 * The count is linked to the cast-time payment, X included: mana paid for {X} is mana spent to cast
 * it, so off-colour mana on X adds colours, while extra mana of a colour already spent does not.
 * The counters are there as it enters, so the 0/0 never meets state-based actions.
 */
class SkyriderElfScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame): Int {
        val elf = game.findPermanent("Skyrider Elf")!!
        return game.state.getEntity(elf)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun gameWithLands(vararg lands: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Skyrider Elf")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        lands.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        context("Skyrider Elf") {

            test("X = 0 paid with {G}{U} enters as a 2/2 flier") {
                val game = gameWithLands("Forest", "Island")

                game.castXSpell(1, "Skyrider Elf", 0).error shouldBe null
                game.resolveStack()

                withClue("two colours spent → two +1/+1 counters, so the 0/0 survives") {
                    game.isOnBattlefield("Skyrider Elf") shouldBe true
                    plusCounters(game) shouldBe 2
                }
                val elf = game.findPermanent("Skyrider Elf")!!
                game.state.projectedState.getPower(elf) shouldBe 2
                game.state.projectedState.getToughness(elf) shouldBe 2
                game.state.projectedState.hasKeyword(elf, Keyword.FLYING) shouldBe true
            }

            test("off-colour mana paid for X adds colours") {
                val game = gameWithLands("Forest", "Island", "Plains", "Swamp")

                game.castXSpell(1, "Skyrider Elf", 2).error shouldBe null
                game.resolveStack()

                withClue("G, U, W and B spent → four counters") {
                    plusCounters(game) shouldBe 4
                }
            }

            test("paying X with a colour already spent adds no counter") {
                val game = gameWithLands("Forest", "Forest", "Island")

                game.castXSpell(1, "Skyrider Elf", 1).error shouldBe null
                game.resolveStack()

                withClue("three mana but only green and blue → two counters") {
                    plusCounters(game) shouldBe 2
                }
            }
        }
    }
}
