package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Eldrazi Repurposer (MH3) — "When you cast this spell and when this creature dies, create a 0/1
 * colorless Eldrazi Spawn creature token…"
 */
class EldraziRepurposerScenarioTest : ScenarioTestBase() {

    init {
        context("Eldrazi Repurposer") {
            test("casting it makes a Spawn, and dying makes another") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Eldrazi Repurposer")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Eldrazi Repurposer").error shouldBe null
                game.resolveStack()

                withClue("Cast trigger made one Spawn") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }
                val repurposer = game.findPermanent("Eldrazi Repurposer")!!

                game.castSpell(1, "Doom Blade", repurposer).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Eldrazi Repurposer") shouldBe true
                withClue("Dies trigger made a second Spawn") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                }
            }

            test("the cast trigger resolves even if the spell is countered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Eldrazi Repurposer")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(2, "Counterspell")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Eldrazi Repurposer").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Counterspell", "Eldrazi Repurposer").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Eldrazi Repurposer") shouldBe true
                withClue("Countered spell never died, but the cast trigger still made a Spawn") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }
            }
        }
    }
}
