package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Secret Salvage (AER #71) — {3}{B}{B} Sorcery.
 *
 *   Exile target nonland card from your graveyard. Search your library for any number of cards
 *   with the same name as that card, reveal them, put them into your hand, then shuffle.
 *
 * The name is read off the target (which is exiled by then) and the search offers only cards
 * sharing that exact name; any number — including none — may be taken.
 */
class SecretSalvageScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers("P1", "P2")
            .withCardInHand(1, "Secret Salvage")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Shock")
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("exiles the target and puts every chosen same-named card from the library into hand") {
            val game = board().build()
            val libraryBolts = game.findCardsInLibrary(1, "Lightning Bolt")

            game.castSpellTargetingGraveyardCard(1, "Secret Salvage", 1, "Lightning Bolt").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as SelectCardsDecision
            // Only the cards named like the exiled target are offered — not Shock or the Mountain.
            decision.options shouldContainExactlyInAnyOrder libraryBolts
            game.selectCards(libraryBolts).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Lightning Bolt") shouldContainExactlyInAnyOrder libraryBolts
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 0
            game.findCardsInLibrary(1, "Shock").size shouldBe 1
            game.isInGraveyard(1, "Secret Salvage") shouldBe true
        }

        test("may find none of the same-named cards and still exiles the target") {
            val game = board().build()

            game.castSpellTargetingGraveyardCard(1, "Secret Salvage", 1, "Lightning Bolt").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision as SelectCardsDecision
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            // The target is still exiled even though the search found nothing.
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.findCardsInLibrary(1, "Lightning Bolt").size shouldBe 2
            game.findCardsInHand(1, "Lightning Bolt").size shouldBe 0
        }

        test("cannot target a land card or a card in an opponent's graveyard") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Secret Salvage")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInGraveyard(1, "Mountain")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Secret Salvage", 1, "Mountain").error shouldNotBe null
            game.castSpellTargetingGraveyardCard(1, "Secret Salvage", 2, "Lightning Bolt").error shouldNotBe null
        }
    }
}
