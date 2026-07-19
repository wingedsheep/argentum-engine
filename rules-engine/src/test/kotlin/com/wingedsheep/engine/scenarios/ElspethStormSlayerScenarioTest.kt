package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Elspeth, Storm Slayer (TDM #11, {3}{W}{W}, Loyalty 5).
 *
 *   If one or more tokens would be created under your control, twice that many of those tokens
 *   are created instead.  ([com.wingedsheep.sdk.scripting.MultiplyTokenCreation])
 *   +1: Create a 1/1 white Soldier creature token.
 *   0: Put a +1/+1 counter on each creature you control. Those creatures gain flying until your
 *      next turn.
 *   −3: Destroy target creature an opponent controls with mana value 3 or greater.
 */
class ElspethStormSlayerScenarioTest : ScenarioTestBase() {

    init {
        context("Elspeth, Storm Slayer") {

            test("+1 creates two Soldier tokens (token-doubling replacement applies to her own ability)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elspeth, Storm Slayer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elspeth = game.findPermanent("Elspeth, Storm Slayer")!!
                game.state = game.state.updateEntity(elspeth) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
                }
                val plus1 = cardRegistry.getCard("Elspeth, Storm Slayer")!!.script.activatedAbilities[0]
                game.execute(ActivateAbility(game.player1Id, elspeth, plus1.id)).error shouldBe null
                game.resolveStack()

                withClue("Token doubling turns the single Soldier into two") {
                    game.findPermanents("Soldier Token").size shouldBe 2
                }
            }

            test("0 puts a +1/+1 counter on each creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elspeth, Storm Slayer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elspeth = game.findPermanent("Elspeth, Storm Slayer")!!
                game.state = game.state.updateEntity(elspeth) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
                }
                val zero = cardRegistry.getCard("Elspeth, Storm Slayer")!!.script.activatedAbilities[1]
                game.execute(ActivateAbility(game.player1Id, elspeth, zero.id)).error shouldBe null
                game.resolveStack()

                fun counters(name: String, controller: Int): Int {
                    val id = game.findPermanents(name).first { pid ->
                        game.state.projectedState.getController(pid) ==
                            (if (controller == 1) game.player1Id else game.player2Id)
                    }
                    return game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                }

                withClue("Your creatures each gain a +1/+1 counter") {
                    counters("Grizzly Bears", 1) shouldBe 1
                    counters("Hill Giant", 1) shouldBe 1
                }
                withClue("The opponent's creature is untouched") {
                    counters("Grizzly Bears", 2) shouldBe 0
                }
            }

            test("−3 destroys an opponent's creature with mana value 3 or greater") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elspeth, Storm Slayer")
                    .withCardOnBattlefield(2, "Hill Giant") // {3}{R}, MV 4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elspeth = game.findPermanent("Elspeth, Storm Slayer")!!
                game.state = game.state.updateEntity(elspeth) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 5))
                }
                val giant = game.findPermanent("Hill Giant")!!
                val minus3 = cardRegistry.getCard("Elspeth, Storm Slayer")!!.script.activatedAbilities[2]
                game.execute(
                    ActivateAbility(
                        game.player1Id, elspeth, minus3.id,
                        targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (MV 4) is destroyed") {
                    game.findPermanent("Hill Giant") shouldBe null
                    game.state.getGraveyard(game.player2Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe true
                }
            }
        }
    }
}
