package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deicide (JOU #7): "Exile target enchantment. If the exiled card is a God card, search its
 * controller's graveyard, hand, and library for any number of cards with the same name as that
 * card and exile them. Then that player shuffles."
 *
 * The God check reads the card in exile, so a God that isn't a creature for lack of devotion still
 * counts; a non-God enchantment is exiled alone, with no search.
 */
class DeicideScenarioTest : ScenarioTestBase() {

    init {
        test("exiling a non-creature God lets the caster exile any number of its namesakes") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Deicide")
                .withLandsOnBattlefield(1, "Plains", 2)
                // No devotion: Heliod is an enchantment, not a creature, on the battlefield.
                .withCardOnBattlefield(2, "Heliod, God of the Sun")
                .withCardInHand(2, "Heliod, God of the Sun")
                .withCardInGraveyard(2, "Heliod, God of the Sun")
                .withCardInLibrary(2, "Heliod, God of the Sun")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val heliod = game.findPermanent("Heliod, God of the Sun")!!
            game.state.projectedState.isCreature(heliod) shouldBe false
            val inHand = game.findCardsInHand(2, "Heliod, God of the Sun").single()
            val inGraveyard = game.findCardsInGraveyard(2, "Heliod, God of the Sun").single()
            val inLibrary = game.findCardsInLibrary(2, "Heliod, God of the Sun").single()

            game.castSpell(1, "Deicide", heliod).error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the caster searches the God's controller's graveyard, hand, and library") {
                search.playerId shouldBe game.player1Id
                search.options shouldContainExactlyInAnyOrder listOf(inHand, inGraveyard, inLibrary)
                search.minSelections shouldBe 0
            }
            // "Any number" — leave the library copy where it is.
            game.selectCards(listOf(inHand, inGraveyard)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.getExile(game.player2Id) shouldContainExactlyInAnyOrder listOf(heliod, inHand, inGraveyard)
            game.state.getLibrary(game.player2Id) shouldContain inLibrary
            game.librarySize(2) shouldBe 2
            game.isInGraveyard(1, "Deicide") shouldBe true
        }

        test("exiling an enchantment that isn't a God searches nothing") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Deicide")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Glorious Anthem")
                .withCardInHand(2, "Glorious Anthem")
                .withCardInGraveyard(2, "Glorious Anthem")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val anthem = game.findPermanent("Glorious Anthem")!!
            game.castSpell(1, "Deicide", anthem).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.getExile(game.player2Id) shouldBe listOf(anthem)
            game.findCardsInHand(2, "Glorious Anthem") shouldHaveSize 1
            game.findCardsInGraveyard(2, "Glorious Anthem") shouldHaveSize 1
        }
    }
}
