package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.MyojinOfInfiniteRage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MyojinOfInfiniteRageScenarioTest : ScenarioTestBase() {

    init {
        test("a hand-cast Myojin is indestructible and spends its counter to destroy all lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Infinite Rage")
                .withLandsOnBattlefield(1, "Mountain", 10)
                .withLandsOnBattlefield(2, "Forest", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Infinite Rage").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Infinite Rage")!!
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 1
            withClue("with a divinity counter on it, the Myojin has indestructible") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfInfiniteRage.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Mountain") shouldBe null
            game.findPermanent("Forest") shouldBe null
            withClue("nonland permanents survive") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanent("Myojin of Infinite Rage") shouldNotBe null
            }
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 0
            withClue("the counter is gone, so the conditional grant goes dark") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }
    }
}
