package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Krumar Initiate and its "{X}{B}, {T}, Pay X life" endure-X ability.
 *
 * Card reference:
 * - Krumar Initiate ({1}{B}, 2/2 Human Cleric):
 *   "{X}{B}, {T}, Pay X life: This creature endures X. Activate only as a sorcery.
 *    (Put X +1/+1 counters on it or create an X/X white Spirit creature token.)"
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.AbilityCost.PayXLife] cost: the X
 * from the `{X}{B}` mana symbol drives both the mana paid and the life paid, and feeds
 * the Endure X amount.
 */
class KrumarInitiateScenarioTest : ScenarioTestBase() {

    init {
        context("Krumar Initiate - endure X via {X}{B}, {T}, Pay X life") {

            test("endure X choosing +1/+1 counters pays X life and X mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate")
                    .withLandsOnBattlefield(1, "Swamp", 3) // {X}{B} with X=2 needs 3 mana
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ability = cardRegistry.getCard("Krumar Initiate")!!.script.activatedAbilities.first()
                val krumarId = game.findPermanent("Krumar Initiate")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumarId,
                        abilityId = ability.id,
                        xValue = 2
                    )
                )
                withClue("Activating Krumar Initiate with X=2 should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Paid 2 life as part of the cost (20 -> 18).
                game.getLifeTotal(1) shouldBe 18

                game.resolveStack()

                // Endure presents a modal choice: option 0 = +1/+1 counters, option 1 = token.
                val decision = game.getPendingDecision()
                decision shouldNotBe null
                (decision is ChooseOptionDecision) shouldBe true
                game.submitDecision(OptionChosenResponse(decision!!.id, 0))
                game.resolveStack()

                // Chose counters: 2 +1/+1 counters on Krumar, no Spirit token.
                val counters = game.state.getEntity(krumarId)?.get<CountersComponent>()
                (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 2
                game.findPermanents("Spirit Token").size shouldBe 0
            }

            test("endure X choosing the token creates an X/X white Spirit") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ability = cardRegistry.getCard("Krumar Initiate")!!.script.activatedAbilities.first()
                val krumarId = game.findPermanent("Krumar Initiate")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumarId,
                        abilityId = ability.id,
                        xValue = 2
                    )
                ).error shouldBe null

                game.resolveStack()

                val decision = game.getPendingDecision()
                (decision is ChooseOptionDecision) shouldBe true
                game.submitDecision(OptionChosenResponse(decision!!.id, 1))
                game.resolveStack()

                // Chose the token: a 2/2 white Spirit, and no counters on Krumar.
                val spirits = game.findPermanents("Spirit Token")
                spirits.size shouldBe 1
                val counters = game.state.getEntity(krumarId)?.get<CountersComponent>()
                (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }

            test("X=0 is payable and costs no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate")
                    .withLandsOnBattlefield(1, "Swamp", 1) // {X}{B} with X=0 needs just {B}
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ability = cardRegistry.getCard("Krumar Initiate")!!.script.activatedAbilities.first()
                val krumarId = game.findPermanent("Krumar Initiate")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumarId,
                        abilityId = ability.id,
                        xValue = 0
                    )
                )
                withClue("Activating Krumar Initiate with X=0 should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // No life paid for X=0.
                game.getLifeTotal(1) shouldBe 20
            }

            test("cannot activate at instant speed (sorcery only)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krumar Initiate")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.DECLARE_BLOCKERS) // not a main phase
                    .build()

                val ability = cardRegistry.getCard("Krumar Initiate")!!.script.activatedAbilities.first()
                val krumarId = game.findPermanent("Krumar Initiate")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krumarId,
                        abilityId = ability.id,
                        xValue = 2
                    )
                )
                withClue("Activating at instant speed should be rejected") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
