package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Winding Constrictor — "If one or more counters would be put on an artifact or creature you
 * control, that many plus one of each of those kinds of counters are put on that permanent instead."
 */
class WindingConstrictorScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Winding Constrictor")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("a counter put on a creature you control becomes two; an opponent's creature is untouched") {
            val game = board().withCardInHand(1, "Battlegrowth").withCardInHand(1, "Battlegrowth").build()
            val (mine, theirs) = game.findAllPermanents("Grizzly Bears")
                .partition { game.state.projectedState.getController(it) == game.player1Id }

            game.castSpell(1, "Battlegrowth", targetId = mine.single()).error shouldBe null
            game.resolveStack()
            game.plusOnes(mine.single()) shouldBe 2

            game.castSpell(1, "Battlegrowth", targetId = theirs.single()).error shouldBe null
            game.resolveStack()
            game.plusOnes(theirs.single()) shouldBe 1
        }

        test("a token created with a +1/+1 counter gets one more") {
            val game = board().withCardInGraveyard(1, "Otterball Antics").build()
            game.castSpellFromGraveyard(1, "Otterball Antics").error shouldBe null
            game.resolveStack()
            game.plusOnes(game.findPermanent("Otter Token")!!) shouldBe 2
        }

        test("with Corpsejack Menace, the player adds before doubling") {
            val game = board()
                .withCardOnBattlefield(1, "Corpsejack Menace")
                .withCardInHand(1, "Battlegrowth")
                .build()
            val mine = game.findAllPermanents("Grizzly Bears")
                .single { game.state.projectedState.getController(it) == game.player1Id }
            game.castSpell(1, "Battlegrowth", targetId = mine).error shouldBe null
            game.resolveStack()
            withClue("(1 + 1) × 2 = 4, the order the creature's controller picks (CR 616.1)") {
                game.plusOnes(mine) shouldBe 4
            }
        }
    }
}
