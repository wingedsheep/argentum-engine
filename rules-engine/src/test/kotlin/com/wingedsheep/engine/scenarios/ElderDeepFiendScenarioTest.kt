package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Elder Deep-Fiend — {8} 5/6 Eldrazi Octopus with flash, emerge {5}{U}{U}, and
 * "When you cast this spell, tap up to four target permanents."
 *
 * Flash is what makes the emerge cast legal at instant speed: emerge itself grants no timing
 * permission, so the spell is cast at its normal timing.
 */
class ElderDeepFiendScenarioTest : ScenarioTestBase() {

    init {
        context("Elder Deep-Fiend") {

            test("emerge cast taps the chosen permanents via its cast trigger") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Elder Deep-Fiend")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    // Emerge {5}{U}{U} reduced by 3 → {2}{U}{U}: four Islands.
                    .withLandsOnBattlefield(1, "Island", 4)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Elder Deep-Fiend", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                // The cast trigger asks for its "up to four target permanents".
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                val bears = game.findPermanent("Grizzly Bears")!!
                val forests = game.findAllPermanents("Forest")
                val chosen = listOf(bears) + forests.take(2)
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to chosen))).error shouldBe null

                game.resolveStack()

                withClue("every chosen permanent is tapped") {
                    for (permanent in chosen) {
                        game.state.getEntity(permanent)
                            ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                    }
                }
                game.isOnBattlefield("Elder Deep-Fiend") shouldBe true
            }

            test("flash lets the emerge cast happen during the opponent's turn") {
                val game = scenario()
                    .withPlayers()
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .withCardInHand(1, "Elder Deep-Fiend")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Elder Deep-Fiend", "Centaur Courser")
                withClue("flash makes the emerge cast legal off-turn: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true
            }
        }
    }
}
