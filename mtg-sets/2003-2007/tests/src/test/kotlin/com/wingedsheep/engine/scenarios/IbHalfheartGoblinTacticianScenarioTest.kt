package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ib Halfheart, Goblin Tactician — "Whenever another Goblin you control becomes blocked, sacrifice
 * it. If you do, it deals 4 damage to each creature blocking it." The blockers are read through
 * `blockingEntity(TriggeringEntity)` and gathered *before* the sacrifice, because a sacrificed
 * attacker leaves combat and nothing is blocking it afterwards.
 */
class IbHalfheartGoblinTacticianScenarioTest : ScenarioTestBase() {

    init {
        test("a blocked Goblin is sacrificed and deals 4 to each of its blockers only") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ib Halfheart, Goblin Tactician")
                .withCardOnBattlefield(1, "Raging Goblin")      // 1/1 Goblin attacker
                .withCardOnBattlefield(1, "Centaur Courser")    // unrelated attacker
                .withCardOnBattlefield(2, "Craw Wurm")          // 6/4 blocks the Goblin
                .withCardOnBattlefield(2, "Hill Giant")         // 3/3 blocks the Goblin
                .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 blocks the Courser
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Raging Goblin" to 2, "Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf(
                    "Craw Wurm" to listOf("Raging Goblin"),
                    "Hill Giant" to listOf("Raging Goblin"),
                    "Grizzly Bears" to listOf("Centaur Courser"),
                )
            )
            game.resolveStack()

            withClue("the blocked Goblin was sacrificed") {
                game.isInGraveyard(1, "Raging Goblin") shouldBe true
            }
            withClue("both of its blockers took 4 damage and died, even though it had left combat") {
                game.isOnBattlefield("Craw Wurm") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
            withClue("the other attacker's blocker is untouched before combat damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            game.isOnBattlefield("Ib Halfheart, Goblin Tactician") shouldBe true
        }

        test("Ib itself becoming blocked does not trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ib Halfheart, Goblin Tactician")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Ib Halfheart, Goblin Tactician" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Ib Halfheart, Goblin Tactician")))

            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Ib Halfheart, Goblin Tactician") shouldBe true
        }

        test("no damage is dealt if the Goblin is gone before the trigger resolves") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ib Halfheart, Goblin Tactician")
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Raging Goblin" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Craw Wurm" to listOf("Raging Goblin")))

            // With the trigger on the stack, the defender Bolts the Goblin in response.
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Raging Goblin")!!).outcome shouldBe Outcome.Done
            game.state.stack.size shouldBe 2
            game.resolveStack()

            game.isInGraveyard(1, "Raging Goblin") shouldBe true
            withClue("the Goblin wasn't sacrificed, so its blocker takes no damage") {
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }
        }

        test("sacrificing two Mountains creates two 1/1 red Goblin tokens") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ib Halfheart, Goblin Tactician")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ib = game.findPermanent("Ib Halfheart, Goblin Tactician")!!
            val mountains = game.findPermanents("Mountain")
            val abilityId = cardRegistry.getCard("Ib Halfheart, Goblin Tactician")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = ib,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = mountains),
                )
            ).outcome shouldBe Outcome.Done
            game.resolveStack()

            game.findPermanents("Mountain").size shouldBe 0
            game.findPermanents("Goblin Token").size shouldBe 2
        }
    }
}
