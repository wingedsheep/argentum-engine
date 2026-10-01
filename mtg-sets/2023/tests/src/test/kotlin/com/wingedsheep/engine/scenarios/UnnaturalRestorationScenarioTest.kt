package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Unnatural Restoration (ONE #191) — {1}{G} Sorcery.
 *
 *   Return target permanent card from your graveyard to your hand. Proliferate.
 */
class UnnaturalRestorationScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Unnatural Restoration")
        .withCardInGraveyard(1, "Centaur Courser")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("returns the targeted permanent card to hand, then proliferates") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpellTargetingGraveyardCard(1, "Unnatural Restoration", 1, "Centaur Courser")
            game.resolveStack()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Centaur Courser") shouldBe false
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.isInGraveyard(1, "Unnatural Restoration") shouldBe true
        }

        test("proliferating nothing still returns the card") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpellTargetingGraveyardCard(1, "Unnatural Restoration", 1, "Centaur Courser")
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Centaur Courser") shouldBe true
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
