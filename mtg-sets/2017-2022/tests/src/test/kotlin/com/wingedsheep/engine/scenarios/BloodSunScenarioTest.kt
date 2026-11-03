package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blood Sun (RIX #92) — "When this enchantment enters, draw a card. All lands lose all abilities
 * except mana abilities."
 */
class BloodSunScenarioTest : ScenarioTestBase() {

    init {
        test("draws a card on entering") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Blood Sun")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Blood Sun").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Blood Sun") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("Rogue's Passage still taps for {C} but can't make a creature unblockable") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Blood Sun")
                .withCardOnBattlefield(1, "Rogue's Passage")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val passage = game.findPermanent("Rogue's Passage")!!
            val (mana, unblockable) = cardRegistry.getCard("Rogue's Passage")!!.script.activatedAbilities
                .partition { it.isManaAbility }.let { it.first.single() to it.second.single() }

            withClue("the opponent's Blood Sun reaches every land") {
                game.getLegalActions(1).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == passage && a.abilityId == unblockable.id } == true
                } shouldBe false
            }
            game.execute(ActivateAbility(game.player1Id, passage, mana.id)).error shouldBe null
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            game.state.getEntity(passage)!!.has<TappedComponent>() shouldBe true
            game.execute(ActivateAbility(game.player1Id, passage, unblockable.id)).error shouldNotBe null
        }

        test("Jungle Hollow played under it enters untapped and gains no life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Blood Sun")
                .withCardInHand(1, "Jungle Hollow")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Jungle Hollow").single())).error shouldBe null
            game.resolveStack()

            val hollow = game.findPermanent("Jungle Hollow")!!
            game.state.getEntity(hollow)!!.has<TappedComponent>() shouldBe false
            game.getLifeTotal(1) shouldBe 20
            withClue("its mana ability survives") {
                game.state.projectedState.hasLostManaAbilities(hollow) shouldBe false
            }
        }
    }
}
