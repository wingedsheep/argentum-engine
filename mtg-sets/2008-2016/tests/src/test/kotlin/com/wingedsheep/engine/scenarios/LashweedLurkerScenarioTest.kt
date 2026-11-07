package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lashweed Lurker (EMN #9) — {8} 5/4 Eldrazi Horror, emerge {5}{G}{U}, and
 * "When you cast this spell, you may put target nonland permanent on top of its owner's library."
 */
class LashweedLurkerScenarioTest : ScenarioTestBase() {

    init {
        context("Lashweed Lurker") {

            test("emerge cast trigger tucks the target on top of its owner's library") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lashweed Lurker")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter") // a second legal target
                    // Emerge {5}{G}{U} reduced by 3 → {2}{G}{U}.
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Lashweed Lurker", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null

                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the Bears went to the top of its owner's library") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.findCardsInLibrary(2, "Grizzly Bears").size shouldBe 1
                    game.isOnBattlefield("Ornithopter") shouldBe true
                }
                game.isOnBattlefield("Lashweed Lurker") shouldBe true
            }

            test("declining the may leaves the target on the battlefield") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Lashweed Lurker")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter") // a second legal target
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .build()

                game.castSpellWithEmerge(1, "Lashweed Lurker", "Centaur Courser").error shouldBe null
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null

                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Lashweed Lurker") shouldBe true
            }
        }
    }
}
