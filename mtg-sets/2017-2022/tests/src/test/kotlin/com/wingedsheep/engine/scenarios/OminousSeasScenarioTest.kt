package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ominous Seas — a foreshadow counter for each card its controller draws,
 * and "Remove eight foreshadow counters" pays for an 8/8 blue Kraken token.
 */
class OminousSeasScenarioTest : ScenarioTestBase() {

    private val krakenAbility get() = cardRegistry.getCard("Ominous Seas")!!.activatedAbilities[0].id

    private fun foreshadow(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.FORESHADOW) ?: 0

    init {
        test("each card you draw adds a foreshadow counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ominous Seas")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val seas = game.findPermanent("Ominous Seas")!!

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()
            withClue("Divination drew two cards: one counter per card") { foreshadow(game, seas) shouldBe 2 }
        }

        test("removing eight foreshadow counters makes an 8/8 blue Kraken; seven isn't enough") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ominous Seas")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val seas = game.findPermanent("Ominous Seas")!!

            game.state = game.state.updateEntity(seas) { c ->
                c.with(CountersComponent().withAdded(CounterType.FORESHADOW, 7))
            }
            withClue("Seven counters can't pay the cost") {
                game.execute(ActivateAbility(game.player1Id, seas, krakenAbility)).error shouldNotBe null
            }

            game.state = game.state.updateEntity(seas) { c ->
                c.with(CountersComponent().withAdded(CounterType.FORESHADOW, 9))
            }
            game.execute(ActivateAbility(game.player1Id, seas, krakenAbility)).error shouldBe null
            withClue("The cost removes exactly eight") { foreshadow(game, seas) shouldBe 1 }
            game.resolveStack()

            val kraken = game.findPermanents("Kraken Token")
            withClue("One Kraken token") { kraken.size shouldBe 1 }
            val projected = game.state.projectedState
            projected.getPower(kraken.single()) shouldBe 8
            projected.getToughness(kraken.single()) shouldBe 8
        }
    }
}
