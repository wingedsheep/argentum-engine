package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Signature Slam (MH3 #168) — "Put a +1/+1 counter on target creature you control, then each
 * modified creature you control deals damage equal to its power to target creature you don't
 * control."
 */
class SignatureSlamScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.giveCounter(id: EntityId) {
        state = state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        }
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Signature Slam")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.slam(mine: EntityId, theirs: EntityId) {
        val card = state.getHand(player1Id).first()
        execute(
            CastSpell(
                player1Id, card,
                listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
            )
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("only the countered creature is modified: it alone deals damage") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.slam(bears, wurm)

            game.plusOnes(bears) shouldBe 1
            withClue("3 from the 3/3 Bears; the unmodified Hill Giant deals nothing") {
                game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
            }
            game.findPermanent("Centaur Courser") shouldNotBe null
        }

        test("every modified creature you control deals damage equal to its power") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.giveCounter(giant)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.slam(bears, wurm)

            withClue("3 (Bears) + 4 (Giant) = 7 kills the 6/4 Wurm") {
                game.findPermanent("Craw Wurm") shouldBe null
            }
        }

        test("an opponent's modified creature is not a damage source") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.giveCounter(courser)
            val wurm = game.findPermanent("Craw Wurm")!!
            game.slam(bears, wurm)

            game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 3
        }
    }
}
