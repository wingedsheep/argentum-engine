package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lasting Tarfire.
 *
 * Lasting Tarfire ({1}{R}): Enchantment
 * At the beginning of each end step, if you put a counter on a creature this turn,
 * this enchantment deals 2 damage to each opponent.
 */
class LastingTarfireScenarioTest : ScenarioTestBase() {

    // Test spell: sorcery that puts a +1/+1 counter on target creature.
    private val addPlusOneCounter = card("Test Plus One Counter") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            val t = target("creature", Targets.Creature)
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
        }
    }

    // Test spell: sorcery that puts a non-P/T counter on target creature.
    private val addStunCounter = card("Test Stun Counter") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            val t = target("creature", Targets.Creature)
            effect = Effects.AddCounters(Counters.STUN, 1, t)
        }
    }

    init {
        cardRegistry.register(addPlusOneCounter)
        cardRegistry.register(addStunCounter)

        context("Lasting Tarfire - intervening-if end step trigger") {

            test("triggers when controller placed a +1/+1 counter on a creature this turn") {
                val game = scenario()
                    .withPlayers("Tarfire Player", "Opponent")
                    .withCardOnBattlefield(1, "Lasting Tarfire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Test Plus One Counter")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Test Plus One Counter", targetId = bear)
                game.resolveStack()

                withClue("Opponent should be at 20 before end step") {
                    game.getLifeTotal(2) shouldBe 20
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent should take 2 damage from Lasting Tarfire") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("triggers when controller placed a -1/-1 counter on a creature this turn") {
                val game = scenario()
                    .withPlayers("Tarfire Player", "Opponent")
                    .withCardOnBattlefield(1, "Lasting Tarfire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Cinder Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val hillGiant = game.findPermanent("Hill Giant")!!
                val cinderStrike = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Cinder Strike"
                }

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        cinderStrike,
                        listOf(ChosenTarget.Permanent(hillGiant)),
                        additionalCostPayment = AdditionalCostPayment(blightTargets = listOf(bear))
                    )
                )
                withClue("Cinder Strike should be cast with blight: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Opponent should be at 20 before end step") {
                    game.getLifeTotal(2) shouldBe 20
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent should take 2 damage from Lasting Tarfire after blight") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("triggers when controller placed a stun counter on a creature this turn") {
                val game = scenario()
                    .withPlayers("Tarfire Player", "Opponent")
                    .withCardOnBattlefield(1, "Lasting Tarfire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Test Stun Counter")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Test Stun Counter", targetId = bear)
                game.resolveStack()

                withClue("Opponent should be at 20 before end step") {
                    game.getLifeTotal(2) shouldBe 20
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent should take 2 damage from Lasting Tarfire after a stun counter") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("does not trigger when no counter was placed on a creature this turn") {
                val game = scenario()
                    .withPlayers("Tarfire Player", "Opponent")
                    .withCardOnBattlefield(1, "Lasting Tarfire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent should be untouched — no counter was placed this turn") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("resets between turns — only counters placed this turn count") {
                val game = scenario()
                    .withPlayers("Tarfire Player", "Opponent")
                    .withCardOnBattlefield(1, "Lasting Tarfire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Test Plus One Counter")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                // Turn 1: place a counter, trigger fires at end step
                game.castSpell(1, "Test Plus One Counter", targetId = bear)
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18

                // Advance into opponent's turn, then back through to end step
                // Without placing any new counter this turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent's end step with no counter placed shouldn't deal damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
