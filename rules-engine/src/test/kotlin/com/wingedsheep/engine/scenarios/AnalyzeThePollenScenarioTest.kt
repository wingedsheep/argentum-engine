package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Analyze the Pollen (MKM #150) — {G} Sorcery.
 *
 * "As an additional cost to cast this spell, you may collect evidence 8.
 *  Search your library for a basic land card. If evidence was collected, instead search your
 *  library for a creature or land card. Reveal that card, put it into your hand, then shuffle."
 *
 * The card is a branch, and the only thing worth testing is that the branch actually swaps the
 * *filter* rather than bolting an extra effect on. Both tests use the same three-card library —
 * one basic land, one nonbasic land, one creature — and assert on the set of cards the search
 * offers, because that is where a wrong implementation shows up:
 *
 *  - the un-upgraded mode must offer the Forest alone. The nonbasic land is Commercial District,
 *    a `Land — Mountain Forest` that has basic land *types* but is not *basic* — so offering it
 *    would mean the filter was reading subtypes rather than the BASIC supertype, which is the
 *    likelier bug than a plain "land" filter;
 *  - the upgraded mode must offer all three. Per the printed ruling the creature-or-land mode
 *    explicitly reaches nonbasic lands, so a naive "basic land OR creature" upgrade is wrong in a
 *    way that only a nonbasic land in the library reveals.
 *
 * Asserting on the offered options rather than only on what ends up in hand is deliberate: picking
 * the Forest succeeds under either filter, so a test that only checked the result would pass on a
 * broken branch.
 */
class AnalyzeThePollenScenarioTest : ScenarioTestBase() {

    /** The three-card library: basic land, nonbasic land, creature. */
    private fun library() = scenario()
        .withPlayers("Botanist", "Opponent")
        .withCardInHand(1, "Analyze the Pollen")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Commercial District")
        .withCardInLibrary(1, "Centaur Courser")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun offeredNames(game: TestGame): List<String> {
        val decision = game.getPendingDecision() as SelectCardsDecision
        return decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
    }

    init {
        test("without evidence it finds a basic land only") {
            val game = library().build()

            game.castSpell(1, "Analyze the Pollen").error shouldBe null
            game.resolveStack()

            withClue("the un-upgraded mode is 'basic land card' — not 'land card'") {
                offeredNames(game) shouldContainExactlyInAnyOrder listOf("Forest")
            }

            game.selectCards(listOf(game.findCardsInLibrary(1, "Forest").single()))
            game.resolveStack()

            game.isInHand(1, "Forest") shouldBe true
        }

        test("collecting evidence 8 upgrades the search to any creature or land — nonbasics included") {
            val game = library()
                // Gurmag Angler (7) + Lightning Bolt (1) = 8 exactly, the threshold.
                .withCardInGraveyard(1, "Gurmag Angler")
                .withCardInGraveyard(1, "Lightning Bolt")
                .build()

            game.castSpellCollectingEvidence(
                1, "Analyze the Pollen", "Gurmag Angler", "Lightning Bolt"
            ).error shouldBe null
            game.resolveStack()

            val expected = listOf("Forest", "Commercial District", "Centaur Courser")
            withClue("the upgraded mode reaches the creature and the nonbasic land too") {
                offeredNames(game) shouldContainExactlyInAnyOrder expected
            }

            game.selectCards(listOf(game.findCardsInLibrary(1, "Centaur Courser").single()))
            game.resolveStack()

            withClue("the evidence is exiled and the creature is in hand") {
                game.isInHand(1, "Centaur Courser") shouldBe true
                game.isInExile(1, "Gurmag Angler") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
        }
    }
}
