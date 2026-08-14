package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Make Your Move (MKM) — "Destroy target artifact, enchantment, or creature with power 4 or greater."
 *
 * The interesting part is the target filter: the "power 4 or greater" clause binds only to the
 * creature branch, and it reads *projected* power, so a pump spell can make an illegal target legal.
 */
class MakeYourMoveScenarioTest : ScenarioTestBase() {

    init {
        context("Make Your Move — a three-branch destroy") {

            test("destroys a creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Make Your Move")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Make Your Move", targetId = wurm).error shouldBe null
                game.resolveStack()

                withClue("the 6/4 is destroyed") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
            }

            test("cannot target a creature with power 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Make Your Move")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("a 2/2 satisfies none of the three branches") {
                    game.castSpell(1, "Make Your Move", targetId = bears).error shouldNotBe null
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("destroys an artifact regardless of power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Make Your Move")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Ornithopter") // 0/2 artifact creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val thopter = game.findPermanent("Ornithopter")!!
                game.castSpell(1, "Make Your Move", targetId = thopter).error shouldBe null
                game.resolveStack()

                withClue("the artifact branch ignores the power clause entirely") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                }
            }

            test("destroys an enchantment regardless of power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Make Your Move")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pacifism = game.findPermanent("Pacifism")!!
                game.castSpell(1, "Make Your Move", targetId = pacifism).error shouldBe null
                game.resolveStack()

                withClue("the Aura is an enchantment, so it is a legal target") {
                    game.isOnBattlefield("Pacifism") shouldBe false
                    game.isInGraveyard(2, "Pacifism") shouldBe true
                }
            }

            test("a creature pumped to power 4 becomes a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Make Your Move")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 -> 5/5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the filter reads projected power, so the now-5/5 Bears qualifies") {
                    game.castSpell(1, "Make Your Move", targetId = bears).error shouldBe null
                    game.resolveStack()
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
