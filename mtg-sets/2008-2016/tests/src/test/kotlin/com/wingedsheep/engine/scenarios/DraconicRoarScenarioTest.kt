package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Draconic Roar — {1}{R} Instant (DTK #134).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand. Draconic
 * Roar deals 3 damage to target creature. If you revealed a Dragon card or controlled a Dragon as you
 * cast this spell, Draconic Roar deals 3 damage to that creature's controller."
 *
 * Copies are covered by SpellCopyCastTimeCaptureTest.
 */
class DraconicRoarScenarioTest : ScenarioTestBase() {

    private fun board(dragonOnBattlefield: Boolean = false, dragonInHand: Boolean = false): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Draconic Roar")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (dragonOnBattlefield) b = b.withCardOnBattlefield(1, "Shivan Dragon")
        if (dragonInHand) b = b.withCardInHand(1, "Shivan Dragon")
        return b.build()
    }

    init {
        test("without a Dragon it only deals 3 to the creature") {
            val game = board()
            game.castSpell(1, "Draconic Roar", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("controlling a Dragon as you cast it also deals 3 to the creature's controller") {
            val game = board(dragonOnBattlefield = true)
            game.castSpell(1, "Draconic Roar", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(2) shouldBe 17
        }

        test("the Dragon is judged as you cast — losing it in response keeps the bonus") {
            val game = board(dragonOnBattlefield = true)
            game.castSpell(1, "Draconic Roar", game.findPermanent("Hill Giant")!!).error shouldBe null
            val dragon = game.findPermanent("Shivan Dragon")!!
            game.state = game.state
                .removeFromZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD), dragon)
                .addToZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), dragon)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
        }

        test("revealing a Dragon card from hand also deals 3 to the creature's controller") {
            val game = board(dragonInHand = true)
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Draconic Roar").single(),
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                    declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                    additionalCostPayment = AdditionalCostPayment(
                        revealedCards = game.findCardsInHand(1, "Shivan Dragon"),
                    ),
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            withClue("the revealed Dragon stays in hand") { game.isInHand(1, "Shivan Dragon") shouldBe true }
        }
    }
}
