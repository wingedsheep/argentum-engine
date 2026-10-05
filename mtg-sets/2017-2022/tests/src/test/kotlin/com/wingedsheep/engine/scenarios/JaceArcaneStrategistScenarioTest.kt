package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.JaceArcaneStrategist
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace, Arcane Strategist (WAR #270, reprinted in J22 #310; {4}{U}{U}, Loyalty 4).
 *
 *   Whenever you draw your second card each turn, put a +1/+1 counter on target creature you control.
 *   +1: Draw a card.
 *   −7: Creatures you control can't be blocked this turn.
 */
class JaceArcaneStrategistScenarioTest : ScenarioTestBase() {

    private val plusOne = JaceArcaneStrategist.activatedAbilities[0].id
    private val minusSeven = JaceArcaneStrategist.activatedAbilities[1].id

    init {
        context("Jace, Arcane Strategist") {

            test("+1 draws the second card of the turn and the trigger counters the chosen creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Arcane Strategist")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Arcane Strategist")!!
                seedLoyalty(game, jace, 4)
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = plusOne)
                ).error shouldBe null
                game.resolveStack()

                withClue("the +1 drew a card") { game.handSize(1) shouldBe handBefore + 1 }
                loyalty(game, jace) shouldBe 5

                withClue("the second-draw trigger asks for a target creature you control") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                plusOnes(game, giant) shouldBe 1
                plusOnes(game, bears) shouldBe 0
                plusOnes(game, game.findPermanent("Wind Drake")!!) shouldBe 0
            }

            test("the trigger fires only on the second draw, not the first or the third") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Arcane Strategist")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Arcane Strategist")!!
                seedLoyalty(game, jace, 4)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = plusOne)
                ).error shouldBe null
                game.resolveStack()

                withClue("the third draw of the turn does not trigger Jace") {
                    game.hasPendingDecision() shouldBe false
                    plusOnes(game, bears) shouldBe 0
                }
            }

            test("−7 makes every creature you control unblockable this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Arcane Strategist")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wall of Air", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wind Drake", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Arcane Strategist")!!
                seedLoyalty(game, jace, 7)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = minusSeven)
                ).error shouldBe null
                game.resolveStack()

                withClue("Jace paid 7 loyalty and died to the state-based check") {
                    game.findPermanent("Jace, Arcane Strategist") shouldBe null
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the effect outlives Jace: the Wall can't block the Bears") {
                    game.declareBlockers(mapOf("Wall of Air" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                withClue("and the Drake can't block the Giant") {
                    game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"))).error shouldNotBe null
                }
            }

            test("−7 also covers a creature that arrives later in the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Arcane Strategist")
                    .withCardInHand(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Wall of Air", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Arcane Strategist")!!
                seedLoyalty(game, jace, 7)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = minusSeven)
                ).error shouldBe null
                game.resolveStack()

                withClue("Raging Goblin wasn't on the battlefield when the −7 resolved") {
                    game.castSpell(1, "Raging Goblin").error shouldBe null
                    game.resolveStack()
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Raging Goblin" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("\"can't be blocked\" changes no characteristic, so it covers latecomers (CR 611.2c)") {
                    game.declareBlockers(mapOf("Wall of Air" to listOf("Raging Goblin"))).error shouldNotBe null
                }
            }

            test("−7 only covers creatures you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Arcane Strategist")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Arcane Strategist")!!
                seedLoyalty(game, jace, 8)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = minusSeven)
                ).error shouldBe null
                game.resolveStack()
                loyalty(game, jace) shouldBe 1

                val projected = game.state.projectedState
                withClue("your creature can't be blocked") {
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
                withClue("the opponent's creature is untouched") {
                    projected.hasKeyword(game.findPermanent("Hill Giant")!!, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                }
            }
        }
    }

    /** The scenario builder places planeswalkers without their entry loyalty, so seed it. */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
}
