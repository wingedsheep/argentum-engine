package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Carth the Lion (MH2 #189) — {2}{B}{G} Legendary Creature — Human Warrior 3/5.
 *
 * "Whenever Carth enters or a planeswalker you control dies, look at the top seven cards of your
 * library. You may reveal a planeswalker card from among them and put it into your hand. Put the
 * rest on the bottom of your library in a random order.
 * Planeswalkers' loyalty abilities you activate cost an additional [+1] to activate."
 */
class CarthTheLionScenarioTest : ScenarioTestBase() {

    private fun TestGame.pickPlaneswalker(name: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        val walker = decision.options.first { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name }
        selectCards(listOf(walker)).error shouldBe null
        resolveStack()
    }

    init {
        test("entering finds a planeswalker in the top seven") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Carth the Lion")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Jace Beleren")
                .withCardInLibrary(1, "Forest")
                .build()

            game.castSpell(1, "Carth the Lion").error shouldBe null
            game.resolveStack()
            game.pickPlaneswalker("Jace Beleren")
            game.isInHand(1, "Jace Beleren") shouldBe true
        }

        test("a planeswalker you control dying digs again") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Carth the Lion")
                .withCardOnBattlefield(1, "Garruk Wildspeaker")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Jace Beleren")
                .withCardInLibrary(1, "Forest")
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Garruk Wildspeaker")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Garruk Wildspeaker") shouldBe true
            game.pickPlaneswalker("Jace Beleren")
            game.isInHand(1, "Jace Beleren") shouldBe true
        }

        test("loyalty abilities cost an additional [+1]") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Carth the Lion")
                .withCardOnBattlefield(1, "Jace Beleren")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()

            val jace = game.findPermanent("Jace Beleren")!!
            val minusOne = cardRegistry.getCard("Jace Beleren")!!.script.activatedAbilities
                .first { it.cost == AbilityCost.Loyalty(-1) }
            game.execute(
                ActivateAbility(game.player1Id, jace, minusOne.id, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player1Id)))
            ).error shouldBe null
            game.resolveStack()
            // [−1] became [0]: Jace keeps all three counters.
            game.state.getEntity(jace)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 3
            game.state.getHand(game.player1Id).size shouldBe 1
        }
    }
}
