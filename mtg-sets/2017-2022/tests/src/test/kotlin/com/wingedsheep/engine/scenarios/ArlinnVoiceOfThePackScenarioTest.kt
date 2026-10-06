package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Arlinn, Voice of the Pack: Wolves and Werewolves you control enter with an additional +1/+1
 * counter — Arlinn's own entry and non-Wolf creatures are untouched.
 */
class ArlinnVoiceOfThePackScenarioTest : ScenarioTestBase() {

    private val arlinnName = "Arlinn, Voice of the Pack"

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("Arlinn enters without a +1/+1 counter, and her -2 Wolf enters with one") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, arlinnName)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, arlinnName)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val arlinn = game.findPermanent(arlinnName).shouldNotBeNull()
            withClue("Arlinn isn't a creature entering; she gets no +1/+1 counter") {
                game.plusOnes(arlinn) shouldBe 0
            }

            val minusTwo = cardRegistry.getCard(arlinnName)!!.activatedAbilities[0].id
            val result = game.execute(ActivateAbility(game.player1Id, arlinn, minusTwo))
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val wolf = game.findPermanent("Wolf Token").shouldNotBeNull()
            withClue("The Wolf token enters with one +1/+1 counter") {
                game.plusOnes(wolf) shouldBe 1
            }
        }

        test("a Wolf creature you cast gets the counter, a non-Wolf doesn't") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, arlinnName)
                .withCardInHand(1, "Cemetery Prowler")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cemetery Prowler").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.plusOnes(game.findPermanent("Cemetery Prowler").shouldNotBeNull()) shouldBe 1
            game.plusOnes(game.findPermanent("Grizzly Bears").shouldNotBeNull()) shouldBe 0
        }
    }
}
