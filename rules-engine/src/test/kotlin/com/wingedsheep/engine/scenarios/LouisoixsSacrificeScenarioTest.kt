package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Louisoix's Sacrifice (FIN).
 *
 * {U} Instant
 * As an additional cost to cast this spell, sacrifice a legendary creature or pay {2}.
 * Counter target activated ability, triggered ability, or noncreature spell.
 *
 * Exercises the sacrifice leg of [com.wingedsheep.sdk.scripting.AdditionalCost.OrPay] (sacrifice
 * path vs pay path) and the union target "(ability) OR (noncreature spell)" that
 * excludes creature spells.
 */
class LouisoixsSacrificeScenarioTest : ScenarioTestBase() {

    // Custom proof cards (no quirky abilities to confound the cost/target checks).
    private val testLegend = card("Louisoix Test Legend") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Avatar"
        power = 2
        toughness = 2
    }
    private val testVanillaCreature = card("Louisoix Test Bear") {
        manaCost = "{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(listOf(testLegend, testVanillaCreature))

        context("sacrifice-or-pay additional cost") {
            test("sacrifice path counters a noncreature spell and sacrifices the legendary creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Louisoix's Sacrifice")
                    .withCardOnBattlefield(2, "Louisoix Test Legend")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Lightning Bolt at Player 2 (a noncreature spell on the stack).
                val boltCardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.execute(CastSpell(game.player1Id, boltCardId, listOf(ChosenTarget.Player(game.player2Id))))
                game.execute(PassPriority(game.player1Id))

                val boltOnStack = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                val legendId = game.state.getBattlefield().first {
                    val c = game.state.getEntity(it)
                    c?.get<CardComponent>()?.name == "Louisoix Test Legend" &&
                        c.get<ControllerComponent>()?.playerId == game.player2Id
                }
                val louisoixId = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Louisoix's Sacrifice"
                }

                // Cast via the sacrifice path (pay {U}, sacrifice the legendary creature).
                val result = game.execute(
                    CastSpell(
                        game.player2Id,
                        louisoixId,
                        listOf(ChosenTarget.Spell(boltOnStack)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(legendId)),
                    )
                )
                withClue("Louisoix's Sacrifice should cast via the sacrifice path: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("The legendary creature should be sacrificed") {
                    game.isInGraveyard(2, "Louisoix Test Legend") shouldBe true
                }

                game.resolveStack()

                withClue("Lightning Bolt should be countered (in Player 1's graveyard)") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("Player 2 took no damage — the bolt never resolved") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("pay path counters a noncreature spell when no legendary creature is sacrificed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Louisoix's Sacrifice")
                    .withLandsOnBattlefield(2, "Island", 3) // enough for {U} + {2}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boltCardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.execute(CastSpell(game.player1Id, boltCardId, listOf(ChosenTarget.Player(game.player2Id))))
                game.execute(PassPriority(game.player1Id))

                val boltOnStack = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                val louisoixId = game.state.getHand(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Louisoix's Sacrifice"
                }

                // Cast via the pay path (no sacrifice payment): the engine adds {2} to the cost.
                val result = game.execute(
                    CastSpell(game.player2Id, louisoixId, listOf(ChosenTarget.Spell(boltOnStack)))
                )
                withClue("Louisoix's Sacrifice should cast via the pay path: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Lightning Bolt should be countered (in Player 1's graveyard)") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("Player 2 took no damage — the bolt never resolved") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("with no legendary creature, only the pay path is castable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Louisoix's Sacrifice")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boltCardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.execute(CastSpell(game.player1Id, boltCardId, listOf(ChosenTarget.Player(game.player2Id))))
                game.execute(PassPriority(game.player1Id))

                val louisoixActions = game.getLegalActions(2)
                    .filter { it.description.startsWith("Cast Louisoix's Sacrifice") }
                withClue("Louisoix's Sacrifice should be castable (pay path)") {
                    louisoixActions.isNotEmpty() shouldBe true
                }
                withClue("No sacrifice path should be offered without a legendary creature") {
                    louisoixActions.none { it.description.endsWith("(Sacrifice)") } shouldBe true
                }
            }
        }

        context("union target excludes creature spells") {
            test("a creature spell on the stack is not a legal target; a noncreature spell is") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Louisoix Test Bear")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(2, "Louisoix's Sacrifice")
                    .withCardOnBattlefield(2, "Louisoix Test Legend")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts a creature spell, then (retaining priority) a noncreature spell.
                game.castSpell(1, "Louisoix Test Bear")
                val boltCardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }
                game.execute(CastSpell(game.player1Id, boltCardId, listOf(ChosenTarget.Player(game.player2Id))))
                game.execute(PassPriority(game.player1Id))

                val creatureSpellId = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Louisoix Test Bear"
                }
                val boltOnStack = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
                }

                val louisoixAction = game.getLegalActions(2)
                    .first { it.description.startsWith("Cast Louisoix's Sacrifice") }
                val validTargets = louisoixAction.validTargets
                withClue("Louisoix's Sacrifice should have valid targets") {
                    (validTargets != null && validTargets.isNotEmpty()) shouldBe true
                }
                withClue("The noncreature spell (Lightning Bolt) should be a legal target") {
                    validTargets!! shouldContain boltOnStack
                }
                withClue("The creature spell should NOT be a legal target") {
                    validTargets!! shouldNotContain creatureSpellId
                }
            }
        }
    }
}
