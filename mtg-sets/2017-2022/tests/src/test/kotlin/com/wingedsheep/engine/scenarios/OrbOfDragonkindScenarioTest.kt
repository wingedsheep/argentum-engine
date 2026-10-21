package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Orb of Dragonkind (AFR #157) — {1}{R} Artifact.
 *
 *   {R}, {T}, Sacrifice this artifact: Look at the top seven cards of your library. You may reveal
 *   a Dragon card from among them and put it into your hand. Put the rest on the bottom of your
 *   library in a random order.
 */
class OrbOfDragonkindScenarioTest : ScenarioTestBase() {

    private val dragon = card("Orb Test Dragon") {
        manaCost = "{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(dragon)

        test("sacrificing the Orb finds a Dragon among the top seven") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Orb of Dragonkind")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Orb Test Dragon")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val orb = game.findPermanent("Orb of Dragonkind")!!
            val tutorAbility = cardRegistry.getCard("Orb of Dragonkind")!!.activatedAbilities[1].id
            val dragonCard = game.findCardsInLibrary(1, "Orb Test Dragon").single()

            game.execute(ActivateAbility(game.player1Id, orb, tutorAbility)).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()
            game.selectCards(listOf(dragonCard)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Orb Test Dragon") shouldBe true
            game.isInGraveyard(1, "Orb of Dragonkind") shouldBe true
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
        }
    }
}
