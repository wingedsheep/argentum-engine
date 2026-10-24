package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Test of Talents (STX #59) — "Counter target instant or sorcery spell. Search its controller's
 * graveyard, hand, and library for any number of cards with the same name as that spell and exile
 * them. That player shuffles, then draws a card for each card exiled from their hand this way."
 *
 * Pins what depends on pipeline order: the name and controller are read off the spell before the
 * counter moves it, the countered card itself is then a graveyard match, and only cards exiled
 * from the hand pay out a draw.
 */
class TestOfTalentsScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Test of Talents")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(2, "Lightning Bolt")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInGraveyard(2, "Lightning Bolt")
            .withCardInLibrary(2, "Lightning Bolt")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.bobExile(name: String): List<EntityId> = state.getExile(player2Id).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        /** Bob Bolts Alice; Alice answers with Test of Talents. Returns the Bolt on the stack. */
        fun TestGame.counterTheBolt(): EntityId {
            val boltOnStackCard = findCardsInHand(2, "Lightning Bolt").first()
            execute(
                CastSpell(player2Id, boltOnStackCard, targets = listOf(ChosenTarget.Player(player1Id)))
            ).error shouldBe null
            passPriority()
            execute(
                CastSpell(
                    player1Id,
                    findCardsInHand(1, "Test of Talents").single(),
                    listOf(ChosenTarget.Spell(boltOnStackCard)),
                )
            ).error shouldBe null
            resolveStack()
            return boltOnStackCard
        }

        test("exiles every chosen Bolt and draws one card per Bolt exiled from hand") {
            val game = board()
            val graveyardBolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
            val libraryBolt = game.findCardsInLibrary(2, "Lightning Bolt").single()
            val countered = game.counterTheBolt()
            val handBolt = game.findCardsInHand(2, "Lightning Bolt").single()

            val select = game.getPendingDecision()
            withClue("Alice chooses among every Bolt Bob has, the countered one included") {
                (select is SelectCardsDecision) shouldBe true
                (select as SelectCardsDecision).options shouldContainExactlyInAnyOrder
                    listOf(countered, graveyardBolt, handBolt, libraryBolt)
                select.minSelections shouldBe 0
                select.playerId shouldBe game.player1Id
            }
            game.selectCards(listOf(countered, graveyardBolt, handBolt, libraryBolt)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.bobExile("Lightning Bolt") shouldContainExactlyInAnyOrder
                listOf(countered, graveyardBolt, handBolt, libraryBolt)
            withClue("One Bolt left Bob's hand, so he draws one card") {
                game.handSize(2) shouldBe 1
                game.findCardsInHand(2, "Mountain").size shouldBe 1
            }
        }

        test("exiling only the countered card draws nothing and leaves the other Bolts") {
            val game = board()
            val countered = game.counterTheBolt()

            game.selectCards(listOf(countered)).error shouldBe null
            game.resolveStack()

            game.bobExile("Lightning Bolt") shouldBe listOf(countered)
            game.findCardsInHand(2, "Lightning Bolt").size shouldBe 1
            game.handSize(2) shouldBe 1
            game.findCardsInGraveyard(2, "Lightning Bolt").size shouldBe 1
            game.findCardsInLibrary(2, "Lightning Bolt").size shouldBe 1
        }
    }
}
