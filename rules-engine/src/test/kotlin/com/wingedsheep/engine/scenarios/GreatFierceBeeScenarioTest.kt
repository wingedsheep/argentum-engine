package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Great Fierce Bee (HOB #73) — {2}{B} Creature — Insect 2/2.
 *
 * "Flying
 *  Whenever one or more other creatures die, scry 1."
 *
 * The trigger is the *batched* death shape, so what these tests pin down is how often it fires:
 * once per death batch (not once per creature), for any player's creatures, and never for the
 * Bee's own death alone.
 */
class GreatFierceBeeScenarioTest : ScenarioTestBase() {

    /** Resolves the stack, answering any scry prompts, and returns how many scrys happened. */
    private fun resolveCountingScrys(g: TestGame): Int {
        var scrys = 0
        var guard = 0
        g.resolveStack()
        while (g.getPendingDecision() != null && guard++ < 12) {
            when (g.getPendingDecision()) {
                is SelectCardsDecision -> {
                    scrys++
                    g.skipSelection() // scry: keep the card on top
                }
                is ReorderLibraryDecision -> g.keepLibraryOrder()
                else -> break
            }
            g.resolveStack()
        }
        return scrys
    }

    init {
        context("Great Fierce Bee") {

            test("a single other creature dying scrys once") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Great Fierce Bee")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = g.findPermanent("Grizzly Bears")!!
                g.castSpell(1, "Lightning Bolt", bears).error shouldBe null

                withClue("an opponent's creature dying still counts — the trigger is unscoped") {
                    resolveCountingScrys(g) shouldBe 1
                }
                g.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("a board wipe killing several creatures — including the Bee — scrys exactly once") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Great Fierce Bee")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Wrath of God")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                g.castSpell(1, "Wrath of God").error shouldBe null

                withClue("batched trigger: one scry for the whole death batch, not one per creature") {
                    resolveCountingScrys(g) shouldBe 1
                }
                withClue("CR 603.10 — the Bee died in the same batch and still saw the other deaths") {
                    g.isOnBattlefield("Great Fierce Bee") shouldBe false
                }
            }

            test("the Bee dying on its own does not scry") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Great Fierce Bee")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bee = g.findPermanent("Great Fierce Bee")!!
                g.castSpell(2, "Lightning Bolt", bee).error shouldBe null

                withClue("\"other creatures\" excludes the source itself") {
                    resolveCountingScrys(g) shouldBe 0
                }
                g.isOnBattlefield("Great Fierce Bee") shouldBe false
            }
        }
    }
}
