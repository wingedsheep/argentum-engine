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
 * Noxious Assault (ONE #176) — {3}{G}{G} Sorcery.
 *
 * "Creatures you control get +2/+2 until end of turn. Whenever a creature blocks this turn, its
 *  controller gets a poison counter."
 *
 * Proof card for the filter-scoped per-blocker block delayed trigger: each declared blocker fires
 * its own trigger, and the poison goes to *that blocker's* controller.
 */
class NoxiousAssaultScenarioTest : ScenarioTestBase() {

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Noxious Assault")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Craw Wurm")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Gray Ogre")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("pumps your creatures only") {
            val game = board()
            game.castSpell(1, "Noxious Assault").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
        }

        test("each blocker gives its controller a poison counter") {
            val game = board()
            game.castSpell(1, "Noxious Assault").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Craw Wurm" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Hill Giant" to listOf("Grizzly Bears"), "Gray Ogre" to listOf("Craw Wurm"))
            ).error shouldBe null
            game.resolveStack()

            withClue("two blockers, two triggers, both poisoning the blocking player") {
                poison(game, game.player2Id) shouldBe 2
            }
            poison(game, game.player1Id) shouldBe 0
        }

        test("no blocks, no poison") {
            val game = board()
            game.castSpell(1, "Noxious Assault").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            poison(game, game.player2Id) shouldBe 0
            game.getLifeTotal(2) shouldBe 16
        }
    }
}
