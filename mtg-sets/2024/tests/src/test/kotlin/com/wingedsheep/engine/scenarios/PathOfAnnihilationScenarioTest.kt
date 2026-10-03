package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Path of Annihilation (MH3 #165) — {3}{G} Enchantment
 *
 *   Devoid
 *   When this enchantment enters, create two 0/1 colorless Eldrazi Spawn creature tokens.
 *   Eldrazi you control have "{T}: Add one mana of any color."
 *   Whenever you cast a creature spell with mana value 7 or greater, you gain 4 life.
 */
class PathOfAnnihilationScenarioTest : ScenarioTestBase() {

    init {
        context("Path of Annihilation") {
            test("entering creates two Eldrazi Spawn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Path of Annihilation")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Path of Annihilation").error shouldBe null
                game.resolveStack()

                game.findPermanent("Path of Annihilation") shouldNotBe null
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
            }

            test("an Eldrazi you control taps for mana of any color") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Path of Annihilation")
                    .withCardOnBattlefield(1, "Decimator of the Provinces")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }

            test("a non-Eldrazi creature gains no mana ability") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Path of Annihilation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("casting a creature spell with mana value 7 gains 4 life") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Path of Annihilation")
                    .withCardInHand(1, "Whiptail Wurm")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Whiptail Wurm").error shouldBe null
                game.state.stack shouldHaveSize 2
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 24
            }

            test("a creature spell with mana value 6 gains no life") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Path of Annihilation")
                    .withCardInHand(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Craw Wurm").error shouldBe null
                game.state.stack shouldHaveSize 1
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
