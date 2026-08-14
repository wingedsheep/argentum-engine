package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario coverage for The Aetherspark's attachment, defender restriction, and granted trigger. */
class TheAethersparkScenarioTest : ScenarioTestBase() {

    init {
        context("The Aetherspark") {

            test("+1 attaches it and puts a +1/+1 counter on the chosen creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Aetherspark")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spark = game.findPermanent("The Aetherspark")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                setLoyalty(game, spark, 4)

                val plusOne = cardRegistry.getCard("The Aetherspark")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = spark,
                        abilityId = plusOne.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("The Aetherspark is attached to the chosen creature") {
                    game.state.getEntity(spark)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("the creature receives the +1/+1 counter and the loyalty cost is paid") {
                    counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    counters(game, spark, CounterType.LOYALTY) shouldBe 5
                }
            }

            test("an attached Aetherspark can't be attacked") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "The Aetherspark", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val spark = game.findPermanent("The Aetherspark")!!
                val giant = game.findPermanent("Hill Giant")!!
                val result = game.execute(
                    DeclareAttackers(game.player2Id, mapOf(giant to spark))
                )

                result.error shouldNotBe null
            }

            test("an unattached Aetherspark can be attacked") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "The Aetherspark")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val spark = game.findPermanent("The Aetherspark")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(
                    DeclareAttackers(game.player2Id, mapOf(giant to spark))
                ).error shouldBe null
            }

            test("equipped creature combat damage during your turn adds that much loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "The Aetherspark", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val spark = game.findPermanent("The Aetherspark")!!
                setLoyalty(game, spark, 4)

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("Grizzly Bears dealt 2 combat damage, so The Aetherspark gains 2 loyalty") {
                    counters(game, spark, CounterType.LOYALTY) shouldBe 6
                }
            }
        }
    }

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { container ->
            container.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0
}
