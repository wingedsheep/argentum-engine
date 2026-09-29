package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tandem Takedown (MOM #208): up to two target creatures you control each get +1/+0 until end of
 * turn, then each deals damage equal to its power to another target creature, planeswalker, or
 * battle.
 */
class TandemTakedownScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(victim: EntityId, dealers: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Tandem Takedown"
            },
            listOf(ChosenTarget.Permanent(victim)) + dealers.map { ChosenTarget.Permanent(it) }
        )
    )

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Tandem Takedown")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("two creatures each get +1/+0 and each deal damage equal to their power") {
            val game = base()
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 3/2
                .withCardOnBattlefield(1, "Hill Giant") // 3/3 -> 4/3
                .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.cast(wurm, listOf(bears, giant)).error shouldBe null
            game.resolveStack()

            // 3 + 4 = 7 damage kills the 6/4 Craw Wurm.
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getPower(giant) shouldBe 4
        }

        test("a single dealer gets +1/+0 and deals boosted damage") {
            val game = base()
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 3/2
                .withCardOnBattlefield(2, "Hill Giant") // 3/3
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(giant, listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 3
        }

        test("the victim may be a creature you control") {
            val game = base()
                .withCardOnBattlefield(1, "Hill Giant") // 3/3 -> 4/3
                .withCardOnBattlefield(1, "Craw Wurm") // 6/4
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.cast(wurm, listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Craw Wurm") shouldBe true
        }

        test("a dealer can't also be the victim, and can't target an opponent's creature as dealer") {
            val game = base()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.cast(bears, listOf(bears)).error shouldNotBe null
            game.cast(bears, listOf(giant)).error shouldNotBe null
        }
    }
}
