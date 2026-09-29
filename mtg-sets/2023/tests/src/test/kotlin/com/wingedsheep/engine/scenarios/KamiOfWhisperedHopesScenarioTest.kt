package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kami of Whispered Hopes (MOM #196) — {2}{G} Creature — Spirit 1/1.
 * If one or more +1/+1 counters would be put on a permanent you control, that many plus one +1/+1
 * counters are put on that permanent instead.
 * {T}: Add X mana of any one color, where X is this creature's power.
 *
 * The replacement covers any permanent you control — an uncrewed (noncreature) Vehicle included —
 * and not an opponent's. The mana ability reads the Kami's current power.
 */
class KamiOfWhisperedHopesScenarioTest : ScenarioTestBase() {

    private val mechanicAbilityId
        get() = cardRegistry.getCard("Daring Mechanic")!!.script.activatedAbilities[0].id

    private val kamiAbilityId
        get() = cardRegistry.getCard("Kami of Whispered Hopes")!!.script.activatedAbilities[0].id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun putCounterWithMechanic(game: TestGame, target: EntityId) {
        val result = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Daring Mechanic")!!,
                abilityId = mechanicAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        game.resolveStack()
    }

    init {
        context("Kami of Whispered Hopes") {

            test("a noncreature permanent you control gets one extra +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kami of Whispered Hopes")
                    .withCardOnBattlefield(1, "Daring Mechanic")
                    .withCardOnBattlefield(1, "Air Response Unit") // uncrewed Vehicle
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val unit = game.findPermanent("Air Response Unit")!!
                game.state.projectedState.isCreature(unit) shouldBe false

                putCounterWithMechanic(game, unit)

                withClue("one counter plus the Kami's one") { plusOneCounters(game, unit) shouldBe 2 }
            }

            test("an opponent's permanent gets no extra counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kami of Whispered Hopes")
                    .withCardOnBattlefield(1, "Daring Mechanic")
                    .withCardOnBattlefield(2, "Air Response Unit")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirUnit = game.findPermanent("Air Response Unit")!!
                putCounterWithMechanic(game, theirUnit)

                plusOneCounters(game, theirUnit) shouldBe 1
            }

            test("tapping adds mana of one chosen color equal to the Kami's power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kami of Whispered Hopes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kami = game.findPermanent("Kami of Whispered Hopes")!!
                game.state = game.state.updateEntity(kami) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                val result = game.execute(
                    ActivateAbility(game.player1Id, kami, kamiAbilityId, manaColorChoice = Color.BLUE)
                )
                withClue("mana ability should succeed: ${result.error}") { result.error shouldBe null }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("power 1 + 2 counters = 3 blue") {
                    pool.blue shouldBe 3
                    pool.total shouldBe 3
                }
            }
        }
    }
}
