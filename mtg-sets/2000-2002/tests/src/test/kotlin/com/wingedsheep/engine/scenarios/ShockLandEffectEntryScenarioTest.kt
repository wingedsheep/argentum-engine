package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ons.cards.WoodedFoothills
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A shock land ("As this land enters, you may pay 2 life. If you don't, it enters tapped.") put onto
 * the battlefield by an effect — here fetched by Wooded Foothills — offers the life payment the same
 * way playing it does, instead of entering tapped unasked.
 */
class ShockLandEffectEntryScenarioTest : ScenarioTestBase() {

    private val fetch = "Wooded Foothills"
    private val shock = "Stomping Ground"

    init {
        fun fetchShock(life: Int = 20): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, fetch)
                .withCardInLibrary(1, shock)
                .withLifeTotal(1, life)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.findPermanent(fetch)!!
            game.execute(
                ActivateAbility(game.player1Id, land, WoodedFoothills.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInLibrary(1, shock)).error shouldBe null
            return game
        }

        fun TestGame.shockTapped() = state.getEntity(findPermanent(shock)!!)?.get<TappedComponent>() != null

        test("paying 2 life puts the fetched shock land onto the battlefield untapped") {
            val game = fetchShock()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            game.shockTapped() shouldBe false
            game.getLifeTotal(1) shouldBe 17
        }

        test("declining puts the fetched shock land onto the battlefield tapped") {
            val game = fetchShock()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null

            game.shockTapped() shouldBe true
            game.getLifeTotal(1) shouldBe 19
        }

        test("a player who can't pay 2 life isn't asked and the land enters tapped") {
            val game = fetchShock(life = 2)

            game.getPendingDecision() shouldBe null
            game.shockTapped() shouldBe true
            game.getLifeTotal(1) shouldBe 1
        }

        test("a search that puts the land onto the battlefield tapped doesn't ask (Farseek)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Farseek")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, shock)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Farseek").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInLibrary(1, shock)).error shouldBe null

            game.getPendingDecision() shouldBe null
            game.shockTapped() shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
