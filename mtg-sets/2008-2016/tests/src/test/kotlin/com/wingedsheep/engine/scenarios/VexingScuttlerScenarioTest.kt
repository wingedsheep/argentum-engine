package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vexing Scuttler (EMN #11) — {8} 4/5 Eldrazi Crab, emerge {6}{U}, and
 * "When you cast this spell, you may return target instant or sorcery card from your graveyard to
 * your hand."
 */
class VexingScuttlerScenarioTest : ScenarioTestBase() {

    init {
        context("Vexing Scuttler") {

            test("emerge cast trigger returns an instant from the graveyard to hand") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Vexing Scuttler")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Lava Spike") // a second legal target
                    // Emerge {6}{U} reduced by 3 → {3}{U}.
                    .withLandsOnBattlefield(1, "Island", 4)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Vexing Scuttler", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bolt)))).error shouldBe null

                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt came back to hand") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                    game.isInGraveyard(1, "Lava Spike") shouldBe true
                }
                game.isOnBattlefield("Vexing Scuttler") shouldBe true
            }
        }
    }
}
