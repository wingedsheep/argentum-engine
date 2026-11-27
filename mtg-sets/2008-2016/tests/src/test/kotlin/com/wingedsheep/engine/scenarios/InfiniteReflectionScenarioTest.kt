package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Infinite Reflection (AVR #61) — {5}{U} Aura. "When this Aura enters attached to a creature, each
 * other nontoken creature you control becomes a copy of that creature. Nontoken creatures you control
 * enter as a copy of enchanted creature."
 *
 * The printed entry-copy replacement's rules are pinned in `PrintedEntryCopyReplacementTest`; this
 * covers the card: the enters trigger turns your other nontoken creatures (not an opponent's) into
 * copies of the enchanted creature, later creatures you cast enter as copies, and moving the Aura in
 * response to the trigger doesn't change what the trigger copies (ruling).
 */
class InfiniteReflectionScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    init {
        context("Infinite Reflection") {

            test("your other nontoken creatures become copies; creatures you cast later enter as copies") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Infinite Reflection")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val goblin = game.findPermanent("Raging Goblin")!!
                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Infinite Reflection", giant).error shouldBe null
                game.resolveStack()

                withClue("the Goblin became a Hill Giant; the opponent's Bears didn't") {
                    game.nameOf(goblin) shouldBe "Hill Giant"
                    game.nameOf(theirBears) shouldBe "Grizzly Bears"
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("the Bears entered as a copy of the enchanted Hill Giant") {
                    game.findPermanents("Hill Giant").size shouldBe 3
                }

                // The copies outlast the Aura (ruling): kill the enchanted Giant, the Aura goes too.
                game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()
                withClue("the Reflection is gone, but the Goblin is still a Hill Giant") {
                    game.findPermanent("Infinite Reflection") shouldBe null
                    game.nameOf(goblin) shouldBe "Hill Giant"
                }
            }

            test("moved to another creature before the trigger resolves, the trigger still copies the first") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardInHand(1, "Infinite Reflection")
                    .withCardInHand(1, "Enchantment Alteration")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val goblin = game.findPermanent("Raging Goblin")!!
                game.castSpell(1, "Infinite Reflection", giant).error shouldBe null
                game.passPriority()
                game.passPriority() // the Aura resolves; its enters trigger goes on the stack
                val reflection = game.findPermanent("Infinite Reflection")!!

                game.castSpell(1, "Enchantment Alteration", reflection).error shouldBe null
                game.passPriority()
                game.passPriority()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the trigger copied the Hill Giant it entered on, not the Bears it moved to") {
                    game.nameOf(goblin) shouldBe "Hill Giant"
                    game.nameOf(bears) shouldBe "Hill Giant"
                }
            }
        }
    }
}
