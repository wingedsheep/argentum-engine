package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.LilianaDeathWielder
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Liliana, Death Wielder (AKH #275, {5}{B}{B}, Loyalty 5).
 *
 *   +2: Put a -1/-1 counter on up to one target creature.
 *   −3: Destroy target creature with a -1/-1 counter on it.
 *   −10: Return all creature cards from your graveyard to the battlefield.
 *
 * The +2 may be activated with no target; the −3 can only target a creature that already
 * carries a -1/-1 counter; the −10 returns creature cards only.
 */
class LilianaDeathWielderScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(LilianaDeathWielder))

        context("Liliana, Death Wielder") {

            test("+2 puts a -1/-1 counter on the target, then −3 destroys it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death Wielder")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death Wielder")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                setLoyalty(game, liliana, 5)
                activate(game, liliana, index = 0, targets = listOf(ChosenTarget.Permanent(bears))) shouldBe null
                game.resolveStack()

                withClue("the Bears carry a -1/-1 counter") {
                    counters(game, bears, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                }
                withClue("+2 took Liliana to 7") { counters(game, liliana, CounterType.LOYALTY) shouldBe 7 }

                setLoyalty(game, liliana, 7)
                activate(game, liliana, index = 1, targets = listOf(ChosenTarget.Permanent(bears))) shouldBe null
                game.resolveStack()

                withClue("the countered Bears are destroyed") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                withClue("−3 took Liliana to 4") { counters(game, liliana, CounterType.LOYALTY) shouldBe 4 }
            }

            test("+2 can be activated with no target") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death Wielder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death Wielder")!!
                setLoyalty(game, liliana, 5)
                activate(game, liliana, index = 0) shouldBe null
                game.resolveStack()

                withClue("+2 still adds loyalty") { counters(game, liliana, CounterType.LOYALTY) shouldBe 7 }
            }

            test("−3 cannot target a creature without a -1/-1 counter") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death Wielder")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death Wielder")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                setLoyalty(game, liliana, 5)

                activate(game, liliana, index = 1, targets = listOf(ChosenTarget.Permanent(bears))) shouldNotBe null
                withClue("the Bears survive") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
            }

            test("−10 returns every creature card from your graveyard, and nothing else") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death Wielder")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInGraveyard(2, "Gravedigger")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death Wielder")!!
                setLoyalty(game, liliana, 10)
                activate(game, liliana, index = 2) shouldBe null
                game.resolveStack()

                withClue("both creature cards returned") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
                withClue("the land stays in the graveyard") { game.isInGraveyard(1, "Swamp") shouldBe true }
                withClue("the opponent's graveyard is untouched") { game.isInGraveyard(2, "Gravedigger") shouldBe true }
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ): String? {
        val ability = cardRegistry.getCard("Liliana, Death Wielder")!!.script.activatedAbilities[index]
        return game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }
}
