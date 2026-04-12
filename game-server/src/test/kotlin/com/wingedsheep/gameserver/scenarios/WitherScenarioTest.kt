package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Wither keyword (CR 702.79).
 *
 * Wither: damage dealt to creatures is dealt in the form of -1/-1 counters.
 * Damage to players is unaffected. Lifelink and damage triggers still fire.
 *
 * Uses Barbed Bloodletter to grant wither via its ETB trigger.
 */
class WitherScenarioTest : ScenarioTestBase() {

    init {
        context("Wither - combat damage places -1/-1 counters") {

            test("creature with wither deals combat damage as -1/-1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Barbed Bloodletter")
                    .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 — survives 3 -1/-1 counters
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!
                val balothId = game.findPermanent("Towering Baloth")!!

                // Cast Barbed Bloodletter (no cast-time targets — it's an artifact)
                game.castSpell(1, "Barbed Bloodletter")
                game.resolveStack() // resolves the spell → ETB trigger fires → pending target decision

                // Select target for ETB trigger (attach + grant wither)
                game.selectTargets(listOf(seekerId))
                game.resolveStack() // resolves the ETB trigger

                // Move to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))

                // Advance to declare blockers step
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Towering Baloth" to listOf("Glory Seeker")))

                // Advance through combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // After combat damage, Towering Baloth should have -1/-1 counters, not regular damage
                // Glory Seeker is 3/4 with equipment, so deals 3 damage as -1/-1 counters
                val balothEntity = game.state.getEntity(balothId)!!
                val counters = balothEntity.get<CountersComponent>()
                counters shouldNotBe null
                counters!!.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 3

                // Should NOT have regular damage marked (wither replaces it with counters)
                val damage = balothEntity.get<DamageComponent>()
                val damageAmount = damage?.amount ?: 0
                damageAmount shouldBe 0
            }

            test("wither damage to players is normal (not counters)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Barbed Bloodletter")
                    .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false) // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                // Cast Barbed Bloodletter, select target for ETB
                game.castSpell(1, "Barbed Bloodletter")
                game.resolveStack()
                game.selectTargets(listOf(seekerId))
                game.resolveStack()

                // Attack unblocked — advance through combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should lose life normally (wither only affects creatures)
                // Glory Seeker is 3/4 with equipment → 3 damage to player
                game.getLifeTotal(2) shouldBe 17 // 20 - 3
            }
        }

        context("Wither - kills creature via -1/-1 counters") {

            test("wither damage kills creature when counters reduce toughness to 0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Barbed Bloodletter")
                    .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Raging Goblin") // 1/1
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                // Cast and attach Barbed Bloodletter
                game.castSpell(1, "Barbed Bloodletter")
                game.resolveStack()
                game.selectTargets(listOf(seekerId))
                game.resolveStack()

                // Attack, opponent blocks 1/1 goblin into 3/4 equipped Glory Seeker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Raging Goblin" to listOf("Glory Seeker")))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Raging Goblin (1/1) gets 3 -1/-1 counters → dies to SBA (toughness 0 or less)
                game.isOnBattlefield("Raging Goblin") shouldBe false
            }
        }
    }
}
