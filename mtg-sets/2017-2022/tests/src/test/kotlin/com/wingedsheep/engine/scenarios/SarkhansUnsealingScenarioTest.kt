package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sarkhan's Unsealing (M19 #155) — "Whenever you cast a creature spell with power 4, 5, or 6,
 * this enchantment deals 4 damage to any target. Whenever you cast a creature spell with power 7
 * or greater, this enchantment deals 4 damage to each opponent and each creature and planeswalker
 * they control."
 */
class SarkhansUnsealingScenarioTest : ScenarioTestBase() {

    init {
        test("casting a power-5 creature deals 4 damage to any target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sarkhan's Unsealing")
                .withCardInHand(1, "Charging Monstrosaur")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Charging Monstrosaur").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.findCardsInGraveyard(2, "Hill Giant") shouldHaveSize 1
            game.getLifeTotal(2) shouldBe 20
            withClue("the creature spell still resolves after the trigger") {
                game.isOnBattlefield("Charging Monstrosaur") shouldBe true
            }
        }

        test("casting a power-6 creature uses the any-target ability, not the sweep") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sarkhan's Unsealing")
                .withCardInHand(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Craw Wurm").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("casting a power-3 creature triggers nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sarkhan's Unsealing")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hill Giant").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("casting a power-7-or-greater creature deals 4 to each opponent and their creatures only") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sarkhan's Unsealing")
                .withCardInHand(1, "Ancient Brontodon")
                .withLandsOnBattlefield(1, "Forest", 8)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Ancient Brontodon").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 20
            game.findCardsInGraveyard(2, "Grizzly Bears") shouldHaveSize 1
            game.findCardsInGraveyard(2, "Craw Wurm") shouldHaveSize 1
            withClue("your own creatures are spared") {
                game.findCardsInGraveyard(1, "Grizzly Bears") shouldHaveSize 0
                game.isOnBattlefield("Ancient Brontodon") shouldBe true
            }
        }
    }
}
