package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for the **haste** and **menace** keyword counters (CR 122.1b / 613.1f).
 *
 * Both are on the CR 122.1b list of keywords a keyword counter can be; they were the last two of the
 * eleven missing from `StateProjector.KEYWORD_COUNTER_MAP`. As with the existing flying / first
 * strike / reach counters, the keyword is granted through projection, so it applies to any permanent
 * regardless of its printed abilities — and, being projected, it stops applying the moment the
 * counter is gone.
 *
 * Backs Super-Adaptoid [MSH 250], which copies keywords off another creature as counters.
 */
class HasteMenaceCounterScenarioTest : ScenarioTestBase() {

    private val markHaste = card("Mark Haste") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Put a haste counter on target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.AddCounters(Counters.HASTE, 1, t)
        }
    }

    private val markMenace = card("Mark Menace") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Put a menace counter on target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.AddCounters(Counters.MENACE, 1, t)
        }
    }

    init {
        cardRegistry.register(markHaste)
        cardRegistry.register(markMenace)

        context("keyword counters") {

            test("a haste counter grants the Haste keyword via projection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = true)
                    .withCardInHand(1, "Mark Haste")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("no haste before the counter") {
                    game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe false
                }

                game.castSpell(1, "Mark Haste", giant).error shouldBe null
                game.resolveStack()

                withClue("a haste counter projects the Haste keyword") {
                    game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                }
            }

            test("a haste counter lets a summoning-sick creature attack the turn it entered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = true)
                    .withCardInHand(1, "Mark Haste")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Mark Haste", giant).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the summoning-sickness attack restriction is lifted by the counter") {
                    game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                }
            }

            test("a menace counter grants the Menace keyword via projection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Mark Menace")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("no menace before the counter") {
                    game.state.projectedState.hasKeyword(giant, Keyword.MENACE) shouldBe false
                }

                game.castSpell(1, "Mark Menace", giant).error shouldBe null
                game.resolveStack()

                withClue("a menace counter projects the Menace keyword") {
                    game.state.projectedState.hasKeyword(giant, Keyword.MENACE) shouldBe true
                }
            }

            test("a menace counter makes the creature unblockable by a single blocker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Mark Menace")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Mark Menace", giant).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("menace (CR 702.111b) requires two or more blockers") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant")))
                        .error shouldNotBe null
                }
            }
        }
    }
}
