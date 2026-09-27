package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.MyojinOfCleansingFire
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MyojinOfCleansingFireScenarioTest : ScenarioTestBase() {

    init {
        test("a hand-cast Myojin is indestructible and spends its counter to destroy all other creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Cleansing Fire")
                .withLandsOnBattlefield(1, "Plains", 8)
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Cleansing Fire").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Cleansing Fire")!!
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 1
            withClue("with a divinity counter on it, the Myojin has indestructible") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfCleansingFire.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("every other creature dies, including the controller's own") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Savannah Lions") shouldBe null
            }
            withClue("the Myojin itself and noncreature permanents survive") {
                game.findPermanent("Myojin of Cleansing Fire") shouldNotBe null
                game.findPermanent("Forest") shouldNotBe null
            }
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 0
            withClue("the counter is gone, so the conditional grant goes dark") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }

        test("a Myojin put onto the battlefield without being cast has no counter and cannot activate") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Myojin of Cleansing Fire")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myojin = game.findPermanent("Myojin of Cleansing Fire")!!
            game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfCleansingFire.activatedAbilities.single().id,
                )
            ).error shouldNotBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }
}
