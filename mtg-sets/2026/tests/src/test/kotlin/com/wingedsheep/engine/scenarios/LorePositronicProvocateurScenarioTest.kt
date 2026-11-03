package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lore, Positronic Provocateur (TRK #246) — {1}{U/B}{U/B} Legendary Artifact Creature — Android Rogue 2/3.
 *
 *   When Lore enters, choose one —
 *   • Target creature gets +1/+0 until end of turn and can't be blocked this turn.
 *   • Surveil 2.
 */
class LorePositronicProvocateurScenarioTest : ScenarioTestBase() {

    private val lore = "Lore, Positronic Provocateur"

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        test("mode one gives the target +1/+0 and it can't be blocked this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, lore)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, lore).error shouldBe null
            game.resolveStack()
            game.chooseMode(0)

            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 2
            projected.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
        }

        test("mode two surveils 2") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, lore)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, lore).error shouldBe null
            game.resolveStack()
            game.chooseMode(1)
            if (game.getPendingDecision() == null) game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 2
            game.selectCards(decision.options).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.graveyardSize(1) shouldBe 2
            game.librarySize(1) shouldBe 1
            game.findPermanent(lore) shouldNotBe null
        }
    }
}
