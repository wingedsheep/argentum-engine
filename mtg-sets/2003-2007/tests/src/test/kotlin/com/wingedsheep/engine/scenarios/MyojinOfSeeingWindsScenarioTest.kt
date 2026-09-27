package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.MyojinOfSeeingWinds
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class MyojinOfSeeingWindsScenarioTest : ScenarioTestBase() {

    init {
        test("a hand-cast Myojin spends its divinity counter to draw a card per permanent you control") {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Seeing Winds")
                .withLandsOnBattlefield(1, "Island", 10)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 3)
            repeat(20) { builder = builder.withCardInLibrary(1, "Island") }
            val game = builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Seeing Winds").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Seeing Winds")!!
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 1
            withClue("with a divinity counter on it, the Myojin has indestructible") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            val handBefore = game.handSize(1)
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfSeeingWinds.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("10 Islands + Grizzly Bears + the Myojin itself; the opponent's permanents don't count") {
                game.handSize(1) shouldBe handBefore + 12
            }
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 0
            withClue("the counter is gone, so the conditional grant goes dark") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }

        test("a Myojin put onto the battlefield without being cast has no counter to remove") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Myojin of Seeing Winds")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myojin = game.findPermanent("Myojin of Seeing Winds")!!
            game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfSeeingWinds.activatedAbilities.single().id,
                )
            )
            withClue("the cost can't be paid without a divinity counter") {
                (result.error != null) shouldBe true
            }
        }
    }
}
