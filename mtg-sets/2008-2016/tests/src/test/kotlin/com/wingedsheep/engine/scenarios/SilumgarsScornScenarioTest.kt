package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silumgar's Scorn — {U}{U} Instant (DTK #78).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand. Counter
 * target spell unless its controller pays {1}. If you revealed a Dragon card or controlled a Dragon
 * as you cast this spell, counter that spell instead."
 */
class SilumgarsScornScenarioTest : ScenarioTestBase() {

    private fun bearsOnStack(scornerDragonOnBattlefield: Boolean = false, scornerDragonInHand: Boolean = false): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Scorner")
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardInHand(2, "Silumgar's Scorn")
            .withLandsOnBattlefield(2, "Island", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (scornerDragonOnBattlefield) b = b.withCardOnBattlefield(2, "Shivan Dragon")
        if (scornerDragonInHand) b = b.withCardInHand(2, "Shivan Dragon")
        return b.build().also { game ->
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.passPriority()
        }
    }

    init {
        test("without the Dragon bonus the spell's controller may pay {1}") {
            val game = bearsOnStack()
            game.castSpellTargetingStackSpell(2, "Silumgar's Scorn", "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("controlling a Dragon as you cast it counters the spell outright") {
            val game = bearsOnStack(scornerDragonOnBattlefield = true)
            game.castSpellTargetingStackSpell(2, "Silumgar's Scorn", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            withClue("no chance to pay — the spell is simply countered") { game.hasPendingDecision() shouldBe false }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("revealing a Dragon card counters the spell outright") {
            val game = bearsOnStack(scornerDragonInHand = true)
            val bears = game.state.stack.single()
            game.execute(
                CastSpell(
                    playerId = game.player2Id,
                    cardId = game.findCardsInHand(2, "Silumgar's Scorn").single(),
                    targets = listOf(ChosenTarget.Spell(bears)),
                    declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                    additionalCostPayment = AdditionalCostPayment(
                        revealedCards = game.findCardsInHand(2, "Shivan Dragon"),
                    ),
                )
            ).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("the revealed Dragon stays in hand") { game.isInHand(2, "Shivan Dragon") shouldBe true }
        }
    }
}
