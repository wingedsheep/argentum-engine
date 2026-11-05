package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jace's Defeat (HOU #38) — {1}{U} Instant.
 *
 *   Counter target blue spell. If it was a Jace planeswalker spell, scry 2.
 *
 * Only a Jace planeswalker spell earns the scry; a blue spell merely named for Jace doesn't.
 */
class JacesDefeatScenarioTest : ScenarioTestBase() {

    init {
        fun board(opponentSpell: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Jace's Defeat")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Forest")
            .withCardInHand(2, opponentSpell)
            .withLandsOnBattlefield(2, "Island", 3)
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("countering a Jace planeswalker spell scries 2") {
            val game = board("Jace Beleren")
            game.castSpell(2, "Jace Beleren").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Jace's Defeat", "Jace Beleren").error shouldBe null
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 2 looks at the top two cards of the caster's library") {
                scry.options shouldHaveSize 2
            }
            game.isInGraveyard(2, "Jace Beleren") shouldBe true
        }

        test("countering a blue spell that isn't a Jace planeswalker doesn't scry") {
            val game = board("Jace's Archivist")
            game.castSpell(2, "Jace's Archivist").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Jace's Defeat", "Jace's Archivist").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(2, "Jace's Archivist") shouldBe true
            game.isOnBattlefield("Jace's Archivist") shouldBe false
            game.isInGraveyard(1, "Jace's Defeat") shouldBe true
        }

        test("a non-blue spell isn't a legal target") {
            val game = board("Grizzly Bears")
            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.passPriority()

            game.castSpellTargetingStackSpell(1, "Jace's Defeat", "Grizzly Bears").error shouldNotBe null
        }
    }
}
