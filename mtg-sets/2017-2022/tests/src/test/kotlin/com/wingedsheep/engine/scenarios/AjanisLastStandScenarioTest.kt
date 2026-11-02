package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ajani's Last Stand (M19 #4).
 *
 * Pins the "you may sacrifice this. If you do" dies trigger (accept and decline) and the
 * opponent-caused discard trigger's intervening "if you control a Plains".
 */
class AjanisLastStandScenarioTest : ScenarioTestBase() {

    init {
        test("a creature you control dying lets you sacrifice it for a 4/4 flying Avatar") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ajani's Last Stand")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ajani's Last Stand") shouldBe true
            val avatars = game.findPermanents("Avatar Token")
            avatars.size shouldBe 1
            game.state.projectedState.getPower(avatars.single()) shouldBe 4
            game.state.projectedState.getToughness(avatars.single()) shouldBe 4
        }

        test("declining keeps the enchantment and makes no token") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ajani's Last Stand")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Ajani's Last Stand") shouldBe true
            game.findPermanents("Avatar Token").size shouldBe 0
        }

        test("an opponent's discard spell makes an Avatar when you control a Plains") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ajani's Last Stand")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ajani's Last Stand") shouldBe true
            val avatars = game.findPermanents("Avatar Token")
            avatars.size shouldBe 1
            game.state.projectedState.getController(avatars.single()) shouldBe game.player1Id
        }

        test("without a Plains the discard trigger makes nothing") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ajani's Last Stand")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(2, "Mind Rot")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ajani's Last Stand") shouldBe true
            game.findPermanents("Avatar Token").size shouldBe 0
        }

        test("discarding it to your own discard spell does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ajani's Last Stand")
                .withCardInHand(1, "Mind Rot")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Mind Rot", 1).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ajani's Last Stand") shouldBe true
            game.findPermanents("Avatar Token").size shouldBe 0
        }
    }
}
