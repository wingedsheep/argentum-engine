package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Drana and Linvala (MOM #222) — opponents' creatures' activated abilities can't be activated;
 * Drana and Linvala has all of them, payable with mana as though it were any color.
 */
class DranaAndLinvalaScenarioTest : ScenarioTestBase() {

    init {
        context("Drana and Linvala") {
            test("opponents can't activate their creatures' abilities, mana abilities included") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drana and Linvala", summoningSickness = false)
                    .withCardOnBattlefield(2, "Frozen Shade", summoningSickness = false)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shade = game.findPermanent("Frozen Shade")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val legal = game.getLegalActions(2)
                withClue("neither the pump nor the mana ability is offered to the opponent") {
                    legal.none {
                        val a = it.action
                        a is ActivateAbility && (a.sourceId == shade || a.sourceId == elves)
                    } shouldBe true
                }
                val shadeAbility = cardRegistry.getCard("Frozen Shade")!!.script.activatedAbilities.single()
                game.execute(ActivateAbility(game.player2Id, shade, shadeAbility.id)).error shouldNotBe null
            }

            test("Drana and Linvala gains the abilities, including mana abilities") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drana and Linvala", summoningSickness = false)
                    .withCardOnBattlefield(2, "Frozen Shade")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drana = game.findPermanent("Drana and Linvala")!!
                val elvesAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.single()
                val legal = game.getLegalActions(1)
                fun offered(abilityId: Any) = legal.any {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == drana && a.abilityId == abilityId
                }
                withClue("the gained mana ability is offered on Drana and Linvala") {
                    offered(elvesAbility.id) shouldBe true
                }
            }

            test("a gained {B} ability is paid with white mana as though it were any color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drana and Linvala", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Frozen Shade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drana = game.findPermanent("Drana and Linvala")!!
                val shadeAbility = cardRegistry.getCard("Frozen Shade")!!.script.activatedAbilities.single()
                game.execute(ActivateAbility(game.player1Id, drana, shadeAbility.id)).error shouldBe null
                game.resolveStack()

                withClue("the pump applies to Drana and Linvala, not the Shade") {
                    game.state.projectedState.getPower(drana) shouldBe 4
                    game.state.projectedState.getToughness(drana) shouldBe 5
                    game.state.projectedState.getPower(game.findPermanent("Frozen Shade")!!) shouldBe 0
                }
            }

            test("a gained {T} damage ability taps Drana and Linvala and deals the damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Drana and Linvala", summoningSickness = false)
                    .withCardOnBattlefield(2, "Onakke Javelineer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drana = game.findPermanent("Drana and Linvala")!!
                val javelin = cardRegistry.getCard("Onakke Javelineer")!!.script.activatedAbilities.single()
                game.execute(
                    ActivateAbility(
                        game.player1Id, drana, javelin.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.state.getEntity(drana)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.findPermanent("Onakke Javelineer")!!)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
