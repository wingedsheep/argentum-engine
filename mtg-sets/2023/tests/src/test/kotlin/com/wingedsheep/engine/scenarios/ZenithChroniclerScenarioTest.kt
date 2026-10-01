package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.ZenithChronicler
import com.wingedsheep.mtg.sets.definitions.rav.cards.Watchwolf
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zenith Chronicler (ONE #246) — {2} Artifact Creature — Phyrexian Construct 3/1.
 *
 * "Whenever a player casts their first multicolored spell each turn, each other player draws a card."
 */
class ZenithChroniclerScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(ZenithChronicler)
        cardRegistry.register(Watchwolf)

        context("Zenith Chronicler") {

            test("an opponent's first multicolored spell makes everyone but the caster draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zenith Chronicler")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInHand(2, "Watchwolf")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p1Hand = game.handSize(1)
                game.castSpell(2, "Watchwolf").error shouldBe null
                game.resolveStack()

                withClue("the Chronicler's controller is an other player, so they draw") {
                    game.handSize(1) shouldBe p1Hand + 1
                }
                withClue("the caster does not draw") { game.handSize(2) shouldBe 0 }
            }

            test("the controller casting a multicolored spell makes only the opponent draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zenith Chronicler")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInHand(1, "Watchwolf")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2Hand = game.handSize(2)
                game.castSpell(1, "Watchwolf").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.handSize(2) shouldBe p2Hand + 1
            }

            test("monocolored spells don't trigger it, and a second multicolored spell doesn't either") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zenith Chronicler")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Watchwolf")
                    .withCardInHand(2, "Watchwolf")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p1Hand = game.handSize(1)
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("a monocolored spell is outside the filter") { game.handSize(1) shouldBe p1Hand }

                game.castSpell(2, "Watchwolf").error shouldBe null
                game.resolveStack()
                withClue("first multicolored spell triggers") { game.handSize(1) shouldBe p1Hand + 1 }

                game.castSpell(2, "Watchwolf").error shouldBe null
                game.resolveStack()
                withClue("second multicolored spell does not") { game.handSize(1) shouldBe p1Hand + 1 }

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("a later monocolored spell does not re-open the window") {
                    game.handSize(1) shouldBe p1Hand + 1
                }
            }
        }
    }
}
