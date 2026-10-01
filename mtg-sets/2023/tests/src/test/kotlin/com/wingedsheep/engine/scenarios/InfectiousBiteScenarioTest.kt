package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Infectious Bite (ONE #172) — {1}{G} Instant.
 *
 *   Target creature you control deals damage equal to its power to target creature you don't
 *   control. Each opponent gets a poison counter.
 */
class InfectiousBiteScenarioTest : ScenarioTestBase() {

    private fun poison(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun cast(game: TestGame, biter: EntityId, victim: EntityId) =
        game.execute(
            CastSpell(
                game.player1Id,
                game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Infectious Bite"
                },
                listOf(ChosenTarget.Permanent(biter), ChosenTarget.Permanent(victim)),
            )
        )

    init {
        test("your creature deals damage equal to its power and each opponent gets a poison counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Infectious Bite")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mine = game.findPermanent("Hill Giant")!!
            val theirs = game.findPermanent("Grizzly Bears")!!

            cast(game, mine, theirs).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Hill Giant") shouldBe mine
            poison(game, game.player2Id) shouldBe 1
            poison(game, game.player1Id) shouldBe 0
        }

        test("damage equal to power, not lethal when power is lower than toughness") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Infectious Bite")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            cast(game, bears, giant).error shouldBe null
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldBe giant
            game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            game.findPermanent("Grizzly Bears") shouldBe bears
            poison(game, game.player2Id) shouldBe 1
        }
    }
}
