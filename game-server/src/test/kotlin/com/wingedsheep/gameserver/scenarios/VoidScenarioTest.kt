package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Void.
 *
 * Void ({3}{B}{R}, Sorcery):
 *   "Choose a number. Destroy all artifacts and creatures with mana value equal to that
 *    number. Then target player reveals their hand and discards all nonland cards with
 *    mana value equal to the number."
 *
 * Exercises the new `Effects.ChooseNumberThen` combinator + `manaValueEqualsX()` filter
 * predicate: the single chosen number drives both the board wipe and the discard.
 */
class VoidScenarioTest : ScenarioTestBase() {

    init {
        // Inline test cards with predictable mana values.
        cardRegistry.register(
            CardDefinition.creature("MV2 Bear", ManaCost.parse("{1}{G}"), setOf(Subtype("Bear")), 2, 2)
        )
        cardRegistry.register(
            CardDefinition.creature("MV3 Ogre", ManaCost.parse("{2}{R}"), setOf(Subtype("Ogre")), 3, 3)
        )
        cardRegistry.register(
            CardDefinition.artifact("MV2 Trinket", ManaCost.parse("{2}"))
        )
        cardRegistry.register(
            CardDefinition.sorcery("MV2 Cantrip", ManaCost.parse("{1}{U}"), "Draw a card.")
        )

        context("Void effect") {
            test("destroys MV-equal artifacts and creatures, discards MV-equal nonland cards") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Void")
                    // Caster needs {3}{B}{R} = 5 mana
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Battlefield: an MV2 creature, MV2 artifact (both should die at number=2),
                    // and an MV3 creature (should survive).
                    .withCardOnBattlefield(2, "MV2 Bear")
                    .withCardOnBattlefield(2, "MV2 Trinket")
                    .withCardOnBattlefield(2, "MV3 Ogre")
                    // Opponent's hand: an MV2 spell (discarded), an MV3 spell (kept), a land (kept).
                    .withCardInHand(2, "MV2 Cantrip")
                    .withCardInHand(2, "MV3 Ogre")
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingPlayer(1, "Void", 2)
                withClue("Void should be cast successfully") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Void should pause for a number choice") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                }

                // Choose 2.
                game.chooseNumber(2)

                // Board wipe: MV2 permanents destroyed, MV3 survives.
                withClue("MV2 Bear should be destroyed") {
                    game.isOnBattlefield("MV2 Bear") shouldBe false
                    game.isInGraveyard(2, "MV2 Bear") shouldBe true
                }
                withClue("MV2 Trinket should be destroyed") {
                    game.isOnBattlefield("MV2 Trinket") shouldBe false
                    game.isInGraveyard(2, "MV2 Trinket") shouldBe true
                }
                withClue("MV3 Ogre should survive on the battlefield") {
                    game.isOnBattlefield("MV3 Ogre") shouldBe true
                }

                // Discard: MV2 nonland card discarded; MV3 spell and land kept.
                withClue("Opponent's MV2 Cantrip should be discarded") {
                    game.isInHand(2, "MV2 Cantrip") shouldBe false
                    game.isInGraveyard(2, "MV2 Cantrip") shouldBe true
                }
                withClue("Opponent's MV3 Ogre (in hand) should be kept") {
                    game.isInHand(2, "MV3 Ogre") shouldBe true
                }
                withClue("Opponent's Forest (a land) should be kept") {
                    game.isInHand(2, "Forest") shouldBe true
                }
            }

            test("choosing a number with no matches destroys and discards nothing") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Void")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "MV2 Bear")
                    .withCardInHand(2, "MV2 Cantrip")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Void", 2)
                game.resolveStack()

                // Choose 5 — nothing on board or in hand has MV 5.
                game.chooseNumber(5)

                withClue("MV2 Bear should survive (chosen number was 5)") {
                    game.isOnBattlefield("MV2 Bear") shouldBe true
                }
                withClue("Opponent's MV2 Cantrip should be kept (chosen number was 5)") {
                    game.isInHand(2, "MV2 Cantrip") shouldBe true
                }
            }
        }
    }
}
