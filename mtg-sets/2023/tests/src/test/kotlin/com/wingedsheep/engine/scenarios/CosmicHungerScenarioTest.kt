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
 * Cosmic Hunger (MOM #182): target creature you control deals damage equal to its power to
 * another target creature, planeswalker, or battle.
 */
class CosmicHungerScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(biter: EntityId, victim: EntityId) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Cosmic Hunger"
            },
            listOf(ChosenTarget.Permanent(biter), ChosenTarget.Permanent(victim))
        )
    )

    init {
        test("your creature deals damage equal to its power to an opposing creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cosmic Hunger")
                .withCardOnBattlefield(1, "Hill Giant") // 3/3
                .withCardOnBattlefield(2, "Hill Giant") // 3/3
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giants = game.findAllPermanents("Hill Giant")
            val mine = giants.first { game.state.projectedState.getController(it) == game.player1Id }
            val theirs = giants.first { it != mine }

            game.cast(mine, theirs).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.findPermanent("Hill Giant") shouldBe mine
            game.isInGraveyard(1, "Cosmic Hunger") shouldBe true
        }

        test("can't target an opponent's creature as the source") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cosmic Hunger")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.cast(giant, bears).error shouldNotBe null
        }

        test("can't choose the same creature as both targets") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cosmic Hunger")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.cast(bears, bears).error shouldNotBe null
        }
    }
}
