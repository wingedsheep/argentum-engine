package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Twisted Riddlekeeper (MH3) — {8} 5/5 flier with emerge {5}{C}{U} and "When you cast this spell,
 * tap up to two target permanents. Put a stun counter on each of them."
 */
class TwistedRiddlekeeperScenarioTest : ScenarioTestBase() {

    init {
        context("Twisted Riddlekeeper") {

            test("emerge cast taps two permanents and stuns each of them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Twisted Riddlekeeper")
                    .withCardOnBattlefield(1, "Centaur Courser") // mana value 3
                    // Emerge {5}{C}{U} reduced by 3 → {2}{C}{U}: Wastes + Island + 2 Islands.
                    .withLandsOnBattlefield(1, "Wastes", 1)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(com.wingedsheep.sdk.core.Phase.PRECOMBAT_MAIN, com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Twisted Riddlekeeper", "Centaur Courser")
                withClue("emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val forest = game.findPermanent("Forest").shouldNotBeNull()
                game.selectTargets(listOf(bears, forest)).error shouldBe null
                game.resolveStack()

                for (id in listOf(bears, forest)) {
                    withClue("target is tapped and stunned") {
                        game.state.getEntity(id)!!.has<TappedComponent>() shouldBe true
                        stun(game, id) shouldBe 1
                    }
                }
                game.isOnBattlefield("Twisted Riddlekeeper") shouldBe true
            }

            test("an already-tapped target still gets a stun counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Twisted Riddlekeeper")
                    .withLandsOnBattlefield(1, "Wastes", 1)
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(com.wingedsheep.sdk.core.Phase.PRECOMBAT_MAIN, com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Twisted Riddlekeeper").error shouldBe null
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                stun(game, bears) shouldBe 1
                game.isOnBattlefield("Twisted Riddlekeeper") shouldBe true
            }
        }
    }

    private fun stun(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
}
