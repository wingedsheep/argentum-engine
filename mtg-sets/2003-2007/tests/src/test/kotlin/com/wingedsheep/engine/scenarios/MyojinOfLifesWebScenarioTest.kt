package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.MyojinOfLifesWeb
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MyojinOfLifesWebScenarioTest : ScenarioTestBase() {

    init {
        test("a hand-cast Myojin is indestructible and spends its counter to put creatures from hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Life's Web")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 9)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Life's Web").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Life's Web")!!
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 1
            withClue("with a divinity counter on it, the Myojin has indestructible") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfLifesWeb.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("only creature cards are offered, and any number may be chosen") {
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                val giant = game.findCardsInHand(1, "Hill Giant").single()
                game.selectCards(listOf(bears, giant)).error shouldBe null
            }

            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Hill Giant") shouldNotBe null
            game.isInHand(1, "Giant Growth") shouldBe true
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 0
            withClue("the counter is gone, so the conditional grant goes dark") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }
    }
}
