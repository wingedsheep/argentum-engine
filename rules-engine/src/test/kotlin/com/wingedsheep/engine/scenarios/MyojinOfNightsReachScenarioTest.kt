package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.MyojinOfNightsReach
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class MyojinOfNightsReachScenarioTest : ScenarioTestBase() {

    init {
        test("a hand-cast Myojin receives a divinity counter and can spend it to empty each opponent's hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Night's Reach")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Night's Reach").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Night's Reach")!!
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 1
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0

            val abilityId = MyojinOfNightsReach.activatedAbilities.single().id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = abilityId,
                )
            ).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 0
            game.state.getEntity(myojin)?.get<CountersComponent>()
                ?.getCount(CounterType.DIVINITY) shouldBe 0
        }

        /**
         * The indestructible clause grants to the Myojin *itself*. It was authored as a bare
         * `GrantKeyword(Keyword.INDESTRUCTIBLE)`, whose filter defaults to attached-scope — the
         * Aura/Equipment shape — so nothing was ever attached to the Spirit and it was never
         * actually indestructible. `CardLinter` now fails the build on that shape.
         */
        test("the divinity counter makes the Myojin itself indestructible, and spending it stops") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Myojin of Night's Reach")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Myojin of Night's Reach").error shouldBe null
            game.resolveStack()

            val myojin = game.findPermanent("Myojin of Night's Reach")!!
            withClue("with a divinity counter on it, the Myojin has indestructible") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myojin,
                    abilityId = MyojinOfNightsReach.activatedAbilities.single().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the counter is gone, so the conditional grant goes dark") {
                game.state.projectedState.hasKeyword(myojin, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }
    }
}
