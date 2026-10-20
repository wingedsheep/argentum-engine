package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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

/**
 * Lesser Werewolf — {B}: If this creature's power is 1 or more, it gets -1/-0 until end of turn
 * and put a -0/-1 counter on target creature blocking or blocked by this creature. Activate only
 * during the declare blockers step.
 *
 * Covers the resolution-time power check (a 0-power Werewolf's activation does nothing), the
 * combat-paired target, and the declare-blockers-only timing.
 */
class LesserWerewolfScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Lesser Werewolf")!!.activatedAbilities[0].id
    }

    private fun TestGame.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ZERO_MINUS_ONE) ?: 0

    init {
        context("Lesser Werewolf") {
            test("shrinks itself and its blocker; does nothing once its power is 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lesser Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Lesser Werewolf" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Lesser Werewolf"))).error shouldBe null
                game.passPriority() // defender passes; attacker gets priority in the same step

                val werewolf = game.findPermanent("Lesser Werewolf")!!
                val giant = game.findPermanent("Hill Giant")!!

                fun activate() {
                    val result = game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = werewolf,
                            abilityId = abilityId,
                            targets = listOf(ChosenTarget.Permanent(giant))
                        )
                    )
                    withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                    game.resolveStack()
                }

                activate()
                withClue("Werewolf gets -1/-0, the Giant gets a -0/-1 counter") {
                    game.state.projectedState.getPower(werewolf) shouldBe 1
                    game.counters(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }

                activate()
                game.state.projectedState.getPower(werewolf) shouldBe 0
                game.counters(giant) shouldBe 2

                activate()
                withClue("At power 0 the ability resolves without effect") {
                    game.state.projectedState.getPower(werewolf) shouldBe 0
                    game.counters(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 1
                }
            }

            test("can't be activated outside the declare blockers step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lesser Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attempt = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Lesser Werewolf")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                    )
                )
                attempt.error shouldNotBe null
            }
        }
    }
}
