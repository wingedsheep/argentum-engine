package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sita Varma, Masked Racer (DFT #223) — {G}{U} Legendary Creature — Human Rogue 2/3.
 *
 * "Exhaust — {X}{G}{G}{U}: Put X +1/+1 counters on Sita Varma. Then you may have the base power and
 *  toughness of each other creature you control become equal to Sita Varma's power until end of
 *  turn."
 *
 * The composition puts a `ForEachInGroup` inside a "you may", and its body reads two different
 * entities at once: `EntityReference.Source` for Sita Varma's power and `EffectTarget.Self` for the
 * creature being rewritten. If those collapsed onto one entity, every creature would set its base
 * P/T to its own power — a silent no-op the card snapshot cannot catch. Hence a 3/3 Hill Giant as
 * the other creature, so a wrong answer and a right one differ at every X. The tests pin:
 *
 * - the copied number is `2 + X`, i.e. the counters landed *before* the base-setting ("Then");
 * - Sita Varma herself is excluded ("each **other** creature");
 * - declining the "may" leaves the team alone but keeps the counters;
 * - the base-setting is "until end of turn" while the counters are not.
 */
class SitaVarmaMaskedRacerScenarioTest : ScenarioTestBase() {

    private val exhaustAbilityId
        get() = cardRegistry.getCard("Sita Varma, Masked Racer")!!.script.activatedAbilities[0].id

    init {
        context("Sita Varma's exhaust ability") {

            test("each other creature's base P/T becomes Sita Varma's post-counter power") {
                val game = sitaGame(lands = 7)
                val sita = game.findPermanent("Sita Varma, Masked Racer")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activateExhaust(sita, x = 3, accept = true)

                val projected = game.state.projectedState
                withClue("2 printed power + 3 counters = 5") {
                    projected.getPower(sita) shouldBe 5
                    projected.getToughness(sita) shouldBe 6
                }
                withClue("Hill Giant's base 3/3 is replaced by 5/5 — Sita Varma's power, not its own") {
                    projected.getPower(giant) shouldBe 5
                    projected.getToughness(giant) shouldBe 5
                }
            }

            test("X = 0 adds no counters and copies Sita Varma's printed power of 2") {
                val game = sitaGame(lands = 4)
                val sita = game.findPermanent("Sita Varma, Masked Racer")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activateExhaust(sita, x = 0, accept = true)

                val projected = game.state.projectedState
                withClue("no counters, so Sita Varma is still a 2/3") {
                    projected.getPower(sita) shouldBe 2
                    projected.getToughness(sita) shouldBe 3
                }
                withClue("the base-set can shrink too — Hill Giant goes from 3/3 to 2/2") {
                    projected.getPower(giant) shouldBe 2
                    projected.getToughness(giant) shouldBe 2
                }
            }

            test("declining the 'may' keeps the counters and leaves the team alone") {
                val game = sitaGame(lands = 7)
                val sita = game.findPermanent("Sita Varma, Masked Racer")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activateExhaust(sita, x = 3, accept = false)

                val projected = game.state.projectedState
                withClue("the counters are not part of the optional clause") {
                    projected.getPower(sita) shouldBe 5
                }
                withClue("Hill Giant keeps its printed 3/3") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                }
            }

            test("the base-setting wears off at end of turn but the counters do not") {
                val game = sitaGame(lands = 7)
                val sita = game.findPermanent("Sita Varma, Masked Racer")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.activateExhaust(sita, x = 3, accept = true)
                game.state.projectedState.getPower(giant) shouldBe 5

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val projected = game.state.projectedState
                withClue("\"until end of turn\" — Hill Giant reverts to 3/3") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                }
                withClue("+1/+1 counters are permanent") {
                    projected.getPower(sita) shouldBe 5
                }
            }
        }
    }

    /** Activate the exhaust ability for [x], pay, then answer its "you may" with [accept]. */
    private fun TestGame.activateExhaust(sita: EntityId, x: Int, accept: Boolean) {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = sita,
                abilityId = exhaustAbilityId,
                xValue = x
            )
        )
        withClue("activation with X=$x should succeed: ${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()

        // The ability resolves up to the "you may", which pauses the stack.
        resolveStack()
        withClue("the optional clause should have raised a yes/no") {
            hasPendingDecision() shouldBe true
        }
        answerYesNo(accept)
        resolveStack()
    }

    /**
     * Sita Varma plus one vanilla 3/3 Hill Giant as the "other creature you control", and [lands]
     * lands — enough for {X}{G}{G}{U} at the X the test uses. Neither creature has summoning
     * sickness, so the ability is activatable on the turn the scenario starts.
     */
    private fun sitaGame(lands: Int): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Sita Varma, Masked Racer", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withLandsOnBattlefield(1, "Forest", lands - 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
}
