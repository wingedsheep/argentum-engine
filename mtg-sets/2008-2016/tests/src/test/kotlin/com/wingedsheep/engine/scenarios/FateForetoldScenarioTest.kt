package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fate Foretold (THS #48) — {1}{U} Enchantment — Aura.
 *
 * "Enchant creature
 *  When this Aura enters, draw a card.
 *  When enchanted creature dies, its controller draws a card."
 *
 * The enters draw goes to the Aura's controller; the dies draw goes to the dead creature's
 * last-known controller, even when that is an opponent of the Aura's controller.
 */
class FateForetoldScenarioTest : ScenarioTestBase() {

    init {
        context("Fate Foretold") {

            test("on an opponent's creature: the Aura's controller draws on entry, the creature's controller on death") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Fate Foretold")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Fate Foretold", giant).error shouldBe null
                game.resolveStack()

                withClue("the enters trigger draws for the Aura's controller") {
                    game.handSize(1) shouldBe 2 // Doom Blade + the drawn card
                    game.handSize(2) shouldBe 0
                }

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                withClue("the dead creature's controller draws, not the Aura's controller") {
                    game.handSize(2) shouldBe 1
                    game.handSize(1) shouldBe 1
                }
            }

            test("on your own creature: you draw when it dies") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Fate Foretold", "Hill Giant")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(2, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 0
            }
        }
    }
}
