package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mockery of Nature (EMN #10) — {9} 6/5 Eldrazi Beast, emerge {7}{G}, and
 * "When you cast this spell, you may destroy target artifact or enchantment."
 */
class MockeryOfNatureScenarioTest : ScenarioTestBase() {

    init {
        context("Mockery of Nature") {

            test("emerge cast trigger destroys the target artifact") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Mockery of Nature")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Ornithopter") // a second legal target
                    // Emerge {7}{G} reduced by 3 → {4}{G}.
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Mockery of Nature", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val ornithopter = game.findAllPermanents("Ornithopter").first()
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(ornithopter)))).error shouldBe null

                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("only the chosen Ornithopter was destroyed") {
                    game.findAllPermanents("Ornithopter").size shouldBe 1
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                }
                game.isOnBattlefield("Mockery of Nature") shouldBe true
            }
        }
    }
}
