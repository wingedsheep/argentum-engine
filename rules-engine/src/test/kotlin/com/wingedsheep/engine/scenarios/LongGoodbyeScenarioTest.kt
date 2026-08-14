package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Long Goodbye (MKM) — "This spell can't be countered. Destroy target creature or planeswalker
 * with mana value 3 or less."
 *
 * Two things worth proving: the mana-value gate on the target, and that "can't be countered"
 * really does survive both a Counterspell and an unpayable ward trigger (the 2024-02-02 ruling).
 */
class LongGoodbyeScenarioTest : ScenarioTestBase() {

    init {
        context("Long Goodbye — uncounterable cheap removal") {

            test("destroys a creature with mana value 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Long Goodbye")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears") // {1}{G}, mana value 2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Long Goodbye", targetId = bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("cannot target a creature with mana value 4 or more") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Long Goodbye")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Craw Wurm") // {4}{G}{G}, mana value 6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                withClue("mana value 6 is outside the filter") {
                    game.castSpell(1, "Long Goodbye", targetId = wurm).error shouldNotBe null
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }

            test("Counterspell cannot counter it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Long Goodbye")
                    .withCardInHand(2, "Counterspell")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Long Goodbye", targetId = bears).error shouldBe null
                game.passPriority() // hand priority to the opponent while Long Goodbye is on the stack
                game.castSpellTargetingStackSpell(2, "Counterspell", "Long Goodbye")
                    .error shouldBe null
                game.resolveStack()

                withClue("Counterspell resolves and does nothing; Long Goodbye still kills the Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Counterspell") shouldBe true
                    game.isInGraveyard(1, "Long Goodbye") shouldBe true
                }
            }

            test("an unpaid ward trigger does not counter it either") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Long Goodbye")
                    // Exactly {1}{B} available: nothing left over for Armored Armadillo's ward {1}.
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Armored Armadillo") // {W}, ward {1}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val armadillo = game.findPermanent("Armored Armadillo")!!
                game.castSpell(1, "Long Goodbye", targetId = armadillo).error shouldBe null
                game.resolveStack()

                withClue("ward tries to counter and is refused, so the Armadillo still dies") {
                    game.isOnBattlefield("Armored Armadillo") shouldBe false
                    game.isInGraveyard(2, "Armored Armadillo") shouldBe true
                }
            }
        }
    }
}
