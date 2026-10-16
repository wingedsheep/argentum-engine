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
 * Corpsejack Menace — "If one or more +1/+1 counters would be put on a creature you control, twice
 * that many +1/+1 counters are put on it instead."
 */
class CorpsejackMenaceScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Corpsejack Menace")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("a +1/+1 counter put on a creature you control becomes two") {
            val game = board().withCardInHand(1, "Battlegrowth").build()
            val menace = game.findPermanent("Corpsejack Menace")!!
            game.castSpell(1, "Battlegrowth", targetId = menace).error shouldBe null
            game.resolveStack()
            game.plusOnes(menace) shouldBe 2
        }

        test("devour's entry counters are doubled") {
            val game = board()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Gluttonous Slime")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Gluttonous Slime").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()
            val slime = game.findPermanent("Gluttonous Slime")!!
            withClue("one creature devoured, devour 1 → 1 counter, doubled") {
                game.plusOnes(slime) shouldBe 2
            }
        }

        test("with Winding Constrictor, a token created with a counter gets (1 + 1) × 2") {
            val game = board()
                .withCardOnBattlefield(1, "Winding Constrictor")
                .withCardInGraveyard(1, "Otterball Antics")
                .build()
            game.castSpellFromGraveyard(1, "Otterball Antics").error shouldBe null
            game.resolveStack()
            val otter = game.findPermanent("Otter Token")!!
            withClue("add before doubling, the order the token's controller picks (CR 616.1)") {
                game.plusOnes(otter) shouldBe 4
            }
        }
    }
}
