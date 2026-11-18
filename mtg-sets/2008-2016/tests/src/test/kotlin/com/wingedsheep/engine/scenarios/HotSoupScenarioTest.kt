package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hot Soup (M15 #219) — {1} Artifact — Equipment.
 *
 *   Equipped creature can't be blocked.
 *   Whenever equipped creature is dealt damage, destroy it.
 *   Equip {3}
 *
 * The first Equipment on `Triggers.attached.isDealtDamage()` (every earlier user was an Aura), and
 * "destroy it" reads the damaged creature off the trigger rather than Hot Soup's current host, so
 * the creature still dies when Hot Soup is destroyed in response.
 */
class HotSoupScenarioTest : ScenarioTestBase() {

    private fun board(vararg hand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Craw Wurm") // 6/4: survives Lightning Bolt's 3 damage
        .withCardAttachedTo(1, "Hot Soup", "Craw Wurm")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .apply { hand.forEach { withCardInHand(1, it) } }
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hot Soup") {
            test("non-lethal damage to the equipped creature destroys it") {
                val game = board("Lightning Bolt")
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Craw Wurm")!!).error shouldBe null
                game.resolveStack()

                withClue("the Wurm survives the 3 damage but Hot Soup's trigger destroys it") {
                    game.isInGraveyard(1, "Craw Wurm") shouldBe true
                }
                withClue("Hot Soup stays on the battlefield, unattached") {
                    game.isOnBattlefield("Hot Soup") shouldBe true
                }
            }

            test("the damaged creature is still destroyed if Hot Soup leaves in response") {
                val game = board("Lightning Bolt", "Shatter")
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Craw Wurm")!!).error shouldBe null
                game.passPriority()
                game.passPriority() // Lightning Bolt resolves; Hot Soup's trigger goes on the stack

                withClue("the trigger is waiting on the stack") {
                    game.state.stack.size shouldBe 1
                }
                game.castSpell(1, "Shatter", game.findPermanent("Hot Soup")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hot Soup") shouldBe true
                withClue("\"destroy it\" names the creature that was dealt damage, not Hot Soup's host") {
                    game.isInGraveyard(1, "Craw Wurm") shouldBe true
                }
            }

            test("the equipped creature can't be blocked") {
                val game = board()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Grizzly Bears can't block the equipped Wurm") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Craw Wurm"))).error shouldNotBe null
                }
            }
        }
    }
}
