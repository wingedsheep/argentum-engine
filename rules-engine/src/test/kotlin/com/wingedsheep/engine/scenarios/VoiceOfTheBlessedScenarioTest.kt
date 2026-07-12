package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Voice of the Blessed (VOW #44) — {W}{W} Creature — Spirit Cleric, 2/2.
 *
 *   Whenever you gain life, put a +1/+1 counter on this creature.
 *   As long as this creature has four or more +1/+1 counters on it, it has flying and vigilance.
 *   As long as this creature has ten or more +1/+1 counters on it, it has indestructible.
 *
 * Exercises the lifegain trigger and both counter-count-gated static abilities.
 */
class VoiceOfTheBlessedScenarioTest : ScenarioTestBase() {

    // A minimal sorcery that gains the caster 1 life, used to drive the lifegain trigger
    // repeatedly without relying on a specific pre-registered lifegain card.
    private val gainOneLife = card("Test Gain One") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    private fun plusOneCounters(game: TestGame): Int {
        val voice = game.findPermanent("Voice of the Blessed")!!
        return game.state.getEntity(voice)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        cardRegistry.register(gainOneLife)

        context("Voice of the Blessed") {

            test("gaining life puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Voice of the Blessed", summoningSickness = false)
                    .withCardInHand(1, "Test Gain One")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Gain One").error shouldBe null
                game.resolveStack()

                withClue("gaining 1 life adds a +1/+1 counter") {
                    plusOneCounters(game) shouldBe 1
                }

                val voice = game.findPermanent("Voice of the Blessed")!!
                withClue("still under 4 counters -> no flying/vigilance yet") {
                    game.state.projectedState.hasKeyword(voice, Keyword.FLYING) shouldBe false
                    game.state.projectedState.hasKeyword(voice, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("with 4 or more +1/+1 counters it has flying and vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Voice of the Blessed", summoningSickness = false)
                    .withCardsInHand(1, "Test Gain One", 4)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                repeat(4) {
                    game.castSpell(1, "Test Gain One").error shouldBe null
                    game.resolveStack()
                }

                withClue("4 lifegain triggers -> 4 counters") {
                    plusOneCounters(game) shouldBe 4
                }

                val voice = game.findPermanent("Voice of the Blessed")!!
                withClue("4+ counters grants flying and vigilance") {
                    game.state.projectedState.hasKeyword(voice, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(voice, Keyword.VIGILANCE) shouldBe true
                }
                withClue("still under 10 counters -> no indestructible yet") {
                    game.state.projectedState.hasKeyword(voice, Keyword.INDESTRUCTIBLE) shouldBe false
                }
            }

            test("with 10 or more +1/+1 counters it also has indestructible") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Voice of the Blessed", summoningSickness = false)
                    .build()

                val voice = game.findPermanent("Voice of the Blessed")!!
                game.state = game.state.updateEntity(voice) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 10))
                }

                withClue("10+ counters grants indestructible (as well as flying/vigilance)") {
                    game.state.projectedState.hasKeyword(voice, Keyword.INDESTRUCTIBLE) shouldBe true
                    game.state.projectedState.hasKeyword(voice, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(voice, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
