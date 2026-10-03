package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Titans' Vanguard (MH3 #206): the cast trigger and the attack trigger each put a +1/+1 counter on
 * every colorless creature you control. Colorless is read through devoid (Skittering Precursor), a
 * green creature is skipped, and the cast trigger resolves before the Vanguard lands, so the
 * Vanguard gets nothing from it.
 */
class TitansVanguardScenarioTest : ScenarioTestBase() {

    init {
        fun plusOne(game: ScenarioTestBase.TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("cast trigger counters colorless creatures you control, not the Vanguard or colored ones") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Titans' Vanguard")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Skittering Precursor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Skittering Precursor")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Titans' Vanguard").error shouldBe null
            game.state.stack.size shouldBe 2
            game.resolveStack()

            val mine = game.state.getBattlefield(game.player1Id)
            val precursor = game.findPermanents("Skittering Precursor").single { it in mine }
            val theirs = game.findPermanents("Skittering Precursor").single { it !in mine }
            plusOne(game, precursor) shouldBe 1
            plusOne(game, theirs) shouldBe 0
            plusOne(game, game.findPermanent("Grizzly Bears")!!) shouldBe 0
            plusOne(game, game.findPermanent("Titans' Vanguard")!!) shouldBe 0
        }

        test("attack trigger counters the Vanguard itself and other colorless creatures") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Titans' Vanguard")
                .withCardOnBattlefield(1, "Skittering Precursor")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Titans' Vanguard" to 2)).error shouldBe null
            game.resolveStack()

            plusOne(game, game.findPermanent("Titans' Vanguard")!!) shouldBe 1
            plusOne(game, game.findPermanent("Skittering Precursor")!!) shouldBe 1
            plusOne(game, game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
