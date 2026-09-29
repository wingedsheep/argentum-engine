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
 * Ozolith, the Shattered Spire (MOM #198) — {1}{G} Legendary Artifact.
 * If one or more +1/+1 counters would be put on an artifact or creature you control, that many
 * plus one +1/+1 counters are put on it instead.
 * {1}{G}, {T}: Put a +1/+1 counter on target artifact or creature you control. Activate only as a
 * sorcery.
 * Cycling {2}
 */
class OzolithTheShatteredSpireScenarioTest : ScenarioTestBase() {

    private val ozolithAbilityId
        get() = cardRegistry.getCard("Ozolith, the Shattered Spire")!!.script.activatedAbilities[0].id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun activate(game: TestGame, target: EntityId) = game.execute(
        ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Ozolith, the Shattered Spire")!!,
            abilityId = ozolithAbilityId,
            targets = listOf(ChosenTarget.Permanent(target))
        )
    )

    init {
        context("Ozolith, the Shattered Spire") {

            test("its ability puts two counters on a creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ozolith, the Shattered Spire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = activate(game, bears)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("one counter plus Ozolith's one") { plusOneCounters(game, bears) shouldBe 2 }
            }

            test("a noncreature artifact you control also gets the extra counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ozolith, the Shattered Spire")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stone = game.findPermanent("Mind Stone")!!
                game.state.projectedState.isCreature(stone) shouldBe false

                val result = activate(game, stone)
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                plusOneCounters(game, stone) shouldBe 2
            }

            test("can only be activated as a sorcery") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ozolith, the Shattered Spire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = activate(game, bears)
                result.error shouldNotBe null
                plusOneCounters(game, bears) shouldBe 0
            }
        }
    }
}
