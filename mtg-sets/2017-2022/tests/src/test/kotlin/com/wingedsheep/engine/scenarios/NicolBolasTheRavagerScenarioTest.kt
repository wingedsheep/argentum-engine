package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nicol Bolas, the Ravager // Nicol Bolas, the Arisen (M19 #218).
 *
 * Covers the sorcery-speed exile-and-return flip landing on the planeswalker face with its printed
 * loyalty, −4 reanimating from an opponent's graveyard under your control, and −12 exiling all but
 * the bottom card of the *target* player's library.
 */
class NicolBolasTheRavagerScenarioTest : ScenarioTestBase() {

    private val front = "Nicol Bolas, the Ravager"
    private val back = "Nicol Bolas, the Arisen"

    private fun loyaltyAbility(change: Int) = cardRegistry.requireCard(back).script.activatedAbilities
        .single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

    private fun TestGame.loyaltyOf(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun TestGame.setLoyalty(id: EntityId, amount: Int) {
        state = state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.LOYALTY to amount))) }
    }

    private fun arisenOnMyTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("{4}{U}{B}{R}: exiles the Ravager and returns him as the Arisen with seven loyalty") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ravager = game.findPermanent(front)!!
            val flip = cardRegistry.requireCard(front).script.activatedAbilities.single().id

            game.execute(ActivateAbility(game.player1Id, ravager, flip)).error shouldBe null
            game.resolveStack()

            game.findPermanent(front) shouldBe null
            val arisen = game.findPermanent(back)
            withClue("Bolas returned transformed") { arisen shouldNotBe null }
            game.loyaltyOf(arisen!!) shouldBe 7
            game.state.projectedState.getController(arisen) shouldBe game.player1Id
        }

        test("−4 puts a creature card from an opponent's graveyard onto the battlefield under your control") {
            val game = arisenOnMyTurn { withCardInGraveyard(2, "Grizzly Bears") }
            val bolas = game.findPermanent(back)!!
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

            game.execute(
                ActivateAbility(
                    game.player1Id, bolas, loyaltyAbility(-4),
                    targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.loyaltyOf(bolas) shouldBe 3
            val reanimated = game.findPermanent("Grizzly Bears")
            reanimated shouldNotBe null
            game.state.projectedState.getController(reanimated!!) shouldBe game.player1Id
        }

        test("−12 exiles all but the bottom card of the target player's library") {
            val game = arisenOnMyTurn {
                withCardInLibrary(2, "Grizzly Bears")
                withCardInLibrary(2, "Hill Giant")
                withCardInLibrary(2, "Swamp")
            }
            val bolas = game.findPermanent(back)!!
            game.setLoyalty(bolas, 12)
            game.librarySize(2) shouldBe 4
            val bottom = game.state.getLibrary(game.player2Id).last()

            game.execute(
                ActivateAbility(
                    game.player1Id, bolas, loyaltyAbility(-12),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player2Id).single() shouldBe bottom
            game.isInExile(2, "Hill Giant") shouldBe true
            withClue("your own library is untouched") { game.librarySize(1) shouldBe 1 }
        }
    }
}
