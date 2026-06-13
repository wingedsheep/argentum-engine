package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the four Secrets of Strixhaven "cast an instant or sorcery" trigger-shape
 * cards. They share the new [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL]
 * context key (mana spent on the *triggering* spell) and the
 * `GameObjectFilter.targetsMatching(...)` Repartee filter (instant/sorcery that targets a creature).
 */
class SosTriggerShapeCardsScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Aberrant Manawurm — +X/+0 where X = mana spent on the triggering spell") {

            test("casting Blaze for X=4 (5 total mana) gives the Wurm +5/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aberrant Manawurm") // 2/5
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears") // a creature to burn
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Aberrant Manawurm")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Wurm starts as a 2/5") {
                    projector.getProjectedPower(game.state, wurm) shouldBe 2
                }

                // Blaze with X=4 → 5 mana spent ({4}{R}); target the opposing creature.
                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack() // resolve the cast-trigger (and Blaze)

                withClue("Wurm gets +X/+0 where X = 5 mana spent → 2+5 = 7 power") {
                    projector.getProjectedPower(game.state, wurm) shouldBe 7
                }
                withClue("Toughness is unchanged (+X/+0)") {
                    projector.getProjectedToughness(game.state, wurm) shouldBe 5
                }
            }
        }

        context("Expressive Firedancer — +1/+1, plus double strike if 5+ mana spent") {

            test("a 1-mana spell gives +1/+1 but no double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Expressive Firedancer") // 2/2
                    .withCardInHand(1, "Lightning Bolt") // {R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dancer = game.findPermanent("Expressive Firedancer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Firedancer gets +1/+1 → 3/3") {
                    projector.getProjectedPower(game.state, dancer) shouldBe 3
                    projector.getProjectedToughness(game.state, dancer) shouldBe 3
                }
                withClue("Only 1 mana spent → no double strike") {
                    projector.project(game.state).hasKeyword(dancer, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }

            test("a 5-mana spell gives +1/+1 AND double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Expressive Firedancer") // 2/2
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dancer = game.findPermanent("Expressive Firedancer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Firedancer gets +1/+1 → 3/3") {
                    projector.getProjectedPower(game.state, dancer) shouldBe 3
                    projector.getProjectedToughness(game.state, dancer) shouldBe 3
                }
                withClue("5 mana spent → gains double strike") {
                    projector.project(game.state).hasKeyword(dancer, Keyword.DOUBLE_STRIKE) shouldBe true
                }
            }
        }

        context("Lecturing Scornmage — Repartee +1/+1 counter only when the spell targets a creature") {

            test("instant targeting a creature puts a +1/+1 counter on the Scornmage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lecturing Scornmage") // 1/1
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scornmage = game.findPermanent("Lecturing Scornmage")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Repartee fires: a +1/+1 counter is placed") {
                    val counters = game.state.getEntity(scornmage)
                        ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 1
                }
            }

            test("instant targeting a player does NOT trigger Repartee") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lecturing Scornmage") // 1/1
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scornmage = game.findPermanent("Lecturing Scornmage")!!

                game.castSpellTargetingPlayer(1, "Lightning Bolt", targetPlayerNumber = 2).error shouldBe null
                game.resolveStack()

                withClue("Spell targeted a player, not a creature → no counter") {
                    val counters = game.state.getEntity(scornmage)
                        ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 0
                }
            }
        }

        context("Forum Necroscribe — Repartee reanimates a creature from your graveyard") {

            test("casting a creature-targeting instant returns a creature card from the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Forum Necroscribe") // 5/4
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Grizzly Bears") // creature card to reanimate
                    .withCardOnBattlefield(2, "Glory Seeker") // a creature to target with Bolt
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gloryseeker = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Lightning Bolt", targetId = gloryseeker).error shouldBe null
                game.resolveStack() // cast-trigger asks for a graveyard target

                val bearsInGy = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.selectTargets(listOf(bearsInGy)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is reanimated onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears left the graveyard") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").isEmpty() shouldBe true
                }
            }
        }
    }
}
