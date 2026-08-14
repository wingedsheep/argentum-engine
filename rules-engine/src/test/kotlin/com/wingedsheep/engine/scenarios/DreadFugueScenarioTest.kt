package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dread Fugue (VOW #107).
 *
 * {B} Sorcery — Cleave {2}{B}
 * "Target player reveals their hand. You choose a nonland card from it [with mana value 2 or less].
 *  That player discards that card."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast is a Duress-style targeted discard limited to cheap spells (nonland, mana value 2
 * or less); paying the cleave cost drops the mana-value cap so you can take any nonland card.
 *
 * The bracket lives inside the *effect* (the caster's choose-a-card filter), not the target line —
 * "target player" is identical in both modes. So `cleaveTarget` is unset and only the effect's
 * `SelectFromCollectionEffect` filter differs. These tests pin both modes:
 *  - printed cast can only take a nonland card with mana value ≤ 2 (an expensive card is not an
 *    offered choice), and
 *  - the cleaved cast can take an expensive nonland card.
 *
 * The caster ("you choose") drives a card-selection decision, resolved with `selectCards`.
 */
class DreadFugueScenarioTest : ScenarioTestBase() {

    init {
        context("Dread Fugue — printed cast (brackets present)") {

            test("caster takes a cheap nonland card; an expensive one is not an eligible choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dread Fugue")
                    .withLandsOnBattlefield(1, "Swamp", 1) // {B}
                    // Target's hand: a cheap nonland (Lightning Bolt, MV 1), an expensive nonland
                    // (Hill Giant, MV 4), and a land (Forest) — only the Bolt is an eligible choice.
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Dread Fugue", 2).error shouldBe null
                game.resolveStack()

                // The caster chooses the only eligible card (nonland, MV ≤ 2): Lightning Bolt.
                val bolt = game.findCardsInHand(2, "Lightning Bolt").first()
                game.selectCards(listOf(bolt))
                game.resolveStack()

                withClue("The chosen cheap nonland is discarded") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                }
                withClue("The expensive nonland and the land were never eligible and remain in hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.isInHand(2, "Forest") shouldBe true
                }
            }
        }

        context("Dread Fugue — cleaved cast (brackets removed)") {

            test("caster can take an expensive nonland card (no mana-value cap)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dread Fugue")
                    .withLandsOnBattlefield(1, "Swamp", 3) // Cleave {2}{B}
                    .withCardInHand(2, "Hill Giant") // MV 4 nonland
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithCleaveTargetingPlayer(1, "Dread Fugue", 2).error shouldBe null
                game.resolveStack()

                // The cleaved cast lifts the mana-value cap, so the expensive Hill Giant is eligible.
                val giant = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giant))
                game.resolveStack()

                withClue("The expensive nonland is discarded by the cleaved cast") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("The land was never an eligible choice and remains in hand") {
                    game.isInHand(2, "Forest") shouldBe true
                }
            }
        }
    }
}
