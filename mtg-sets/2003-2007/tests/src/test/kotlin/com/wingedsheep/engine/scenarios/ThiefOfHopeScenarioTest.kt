package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thief of Hope (CHK #147) — "Whenever you cast a Spirit or Arcane spell, target opponent loses 1
 * life and you gain 1 life. Soulshift 2."
 *
 * The soulshift tests pin CR 702.46a: only a Spirit card, with mana value 2 or less, in *your*
 * graveyard is a legal target; the return is optional; with no legal target nothing is asked.
 */
class ThiefOfHopeScenarioTest : ScenarioTestBase() {

    /** Alice bolts her own Thief of Hope, then settles the bolt so the soulshift trigger is pending. */
    private fun TestGame.boltTheThief() {
        val thief = findPermanent("Thief of Hope")!!
        castSpell(1, "Lightning Bolt", thief).error shouldBe null
        resolveStack()
        withClue("Thief of Hope should have died") {
            isInGraveyard(1, "Thief of Hope") shouldBe true
        }
    }

    private fun thiefBoard() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Thief of Hope")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Thief of Hope") {

            test("carries the Soulshift 2 keyword") {
                val game = thiefBoard().build()
                val def = cardRegistry.getCard("Thief of Hope")!!
                def.keywordAbilities shouldContainExactly listOf(KeywordAbility.Numeric(Keyword.SOULSHIFT, 2))
            }

            test("casting a Spirit spell drains the opponent for 1") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Thief of Hope")
                    .withCardInHand(1, "Soilshaper")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Soilshaper").error shouldBe null
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(game.player2Id))
                }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 21
            }

            test("soulshift returns a Spirit card with mana value 2 or less from your graveyard") {
                val game = thiefBoard()
                    .withCardInGraveyard(1, "Soilshaper")       // Spirit, MV 2 — legal
                    .withCardInGraveyard(1, "Kami of the Hunt") // Spirit, MV 3 — too big
                    .withCardInGraveyard(1, "Grizzly Bears")    // MV 2, not a Spirit
                    .withCardInGraveyard(2, "Soilshaper")       // not your graveyard
                    .build()

                game.boltTheThief()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val soilshaper = game.findCardsInGraveyard(1, "Soilshaper").single()
                withClue("only Alice's MV-2 Spirit is legal — not the MV-3 Spirit, the Bears, Bob's card, or the Thief itself") {
                    decision.legalTargets[0].orEmpty() shouldContainExactly listOf(soilshaper)
                }
                game.selectTargets(listOf(soilshaper))
                game.resolveStack()

                game.isInHand(1, "Soilshaper") shouldBe true
                game.isInGraveyard(1, "Kami of the Hunt") shouldBe true
            }

            test("the return is optional") {
                val game = thiefBoard()
                    .withCardInGraveyard(1, "Soilshaper")
                    .build()

                game.boltTheThief()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Soilshaper") shouldBe true
                game.isInHand(1, "Soilshaper") shouldBe false
            }

            test("with no legal Spirit card nothing is asked") {
                val game = thiefBoard()
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .build()

                game.boltTheThief()

                withClue("no pending decision expected, got ${game.getPendingDecision()}") {
                    game.hasPendingDecision() shouldBe false
                }
                game.state.stack.isEmpty() shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
