package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.DakraMystic
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dakra Mystic: "{U}, {T}: Each player reveals the top card of their library. You may put the
 * revealed cards into their owners' graveyards. If you don't, each player draws a card."
 *
 * Pins the controller-only, all-or-nothing choice made after every player has revealed: the
 * mixed-owner revealed collection survives the yes/no pause, "yes" sends each card to its own
 * owner's graveyard, and "no" makes every player draw the revealed card.
 */
class DakraMysticScenarioTest : ScenarioTestBase() {

    private val abilityId = DakraMystic.activatedAbilities.first().id

    private fun activateMystic(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Dakra Mystic", summoningSickness = false)
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Dakra Mystic")!!,
                abilityId = abilityId,
            )
        ).error shouldBe null
        game.resolveStack()

        val decision = game.getPendingDecision()
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe game.player1Id
        return game
    }

    init {
        test("choosing yes puts each revealed card into its owner's graveyard; nobody draws") {
            val game = activateMystic()
            game.answerYesNo(true).error shouldBe null

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.handSize(1) shouldBe 0
            game.handSize(2) shouldBe 0
        }

        test("choosing no leaves the revealed cards on top and each player draws one") {
            val game = activateMystic()
            game.answerYesNo(false).error shouldBe null

            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInHand(2, "Hill Giant").size shouldBe 1
            game.graveyardSize(1) shouldBe 0
            game.graveyardSize(2) shouldBe 0
        }
    }
}
