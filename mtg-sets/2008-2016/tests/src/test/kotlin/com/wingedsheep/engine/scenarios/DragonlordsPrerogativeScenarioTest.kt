package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dragonlord's Prerogative — {4}{U}{U} Instant (DTK #52).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand.
 * If you revealed a Dragon card or controlled a Dragon as you cast this spell, this spell can't be
 * countered. Draw four cards."
 *
 * Each case casts Prerogative, answers it with Cancel, and checks whether Prerogative survived to
 * draw its four cards.
 */
class DragonlordsPrerogativeScenarioTest : ScenarioTestBase() {

    private fun setup(
        dragonOnBattlefield: Boolean = false,
        dragonInHand: Boolean = false,
        countererHasMurder: Boolean = false,
    ): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Counterer")
            .withCardInHand(1, "Dragonlord's Prerogative")
            .withLandsOnBattlefield(1, "Island", 6)
            .withCardInHand(2, "Cancel")
            .withLandsOnBattlefield(2, "Island", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { b = b.withCardInLibrary(1, "Island") }
        if (dragonOnBattlefield) b = b.withCardOnBattlefield(1, "Shivan Dragon")
        if (dragonInHand) b = b.withCardInHand(1, "Shivan Dragon")
        if (countererHasMurder) {
            b = b.withCardInHand(2, "Murder").withLandsOnBattlefield(2, "Swamp", 3)
        }
        return b.build()
    }

    private fun TestGame.cancelPrerogativeAndResolve() {
        passPriority()
        castSpellTargetingStackSpell(2, "Cancel", "Dragonlord's Prerogative").error shouldBe null
        resolveStack()
    }

    init {
        test("without the Dragon bonus Cancel counters it") {
            val game = setup()
            game.castSpell(1, "Dragonlord's Prerogative").error shouldBe null
            game.cancelPrerogativeAndResolve()

            game.isInGraveyard(1, "Dragonlord's Prerogative") shouldBe true
            withClue("countered, so nothing was drawn") { game.handSize(1) shouldBe 0 }
        }

        test("a Dragon card merely in hand, not revealed, gives no protection") {
            val game = setup(dragonInHand = true)
            game.castSpell(1, "Dragonlord's Prerogative").error shouldBe null
            game.cancelPrerogativeAndResolve()

            game.isInGraveyard(1, "Dragonlord's Prerogative") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("revealing a Dragon card makes it uncounterable") {
            val game = setup(dragonInHand = true)
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Dragonlord's Prerogative").single(),
                    declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                    additionalCostPayment = AdditionalCostPayment(
                        revealedCards = game.findCardsInHand(1, "Shivan Dragon"),
                    ),
                )
            ).error shouldBe null
            game.cancelPrerogativeAndResolve()

            game.isInGraveyard(2, "Cancel") shouldBe true
            game.isInGraveyard(1, "Dragonlord's Prerogative") shouldBe true
            withClue("Prerogative resolved: the revealed Dragon stays in hand plus four drawn cards") {
                game.handSize(1) shouldBe 5
            }
        }

        test("controlling a Dragon as you cast it makes it uncounterable") {
            val game = setup(dragonOnBattlefield = true)
            game.castSpell(1, "Dragonlord's Prerogative").error shouldBe null
            game.cancelPrerogativeAndResolve()

            withClue("Prerogative resolved and drew four") { game.handSize(1) shouldBe 4 }
        }

        test("the Dragon counts as the spell is cast, even if it is gone when the counter resolves") {
            val game = setup(dragonOnBattlefield = true, countererHasMurder = true)
            game.castSpell(1, "Dragonlord's Prerogative").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(2, "Cancel", "Dragonlord's Prerogative").error shouldBe null
            game.castSpell(2, "Murder", game.findPermanent("Shivan Dragon")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Shivan Dragon") shouldBe true
            game.isInGraveyard(2, "Cancel") shouldBe true
            withClue("Cancel found the Dragon gone, but the bonus was locked in as Prerogative was cast") {
                game.handSize(1) shouldBe 4
            }
        }
    }
}
