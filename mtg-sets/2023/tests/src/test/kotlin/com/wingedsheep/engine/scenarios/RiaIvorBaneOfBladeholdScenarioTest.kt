package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ria Ivor, Bane of Bladehold (ONE #214) — {2}{W}{B} 3/4 Legendary Creature — Phyrexian Knight.
 *
 * "Battle cry / At the beginning of combat on your turn, the next time target creature would deal
 *  combat damage to one or more players this combat, prevent that damage. If damage is prevented
 *  this way, create that many 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1
 *  and "This token can't block.""
 *
 * Pins the players-only, combat-only prevent-and-react shield: damage to a player is prevented and
 * paid out as Mites; damage to a blocker neither is prevented nor spends the shield, which then
 * lapses at end of combat.
 */
class RiaIvorBaneOfBladeholdScenarioTest : ScenarioTestBase() {

    private fun TestGame.shieldCount(): Int = state.floatingEffects.count {
        it.effect.modification is SerializableModification.PreventNextDamageFromSourceShield
    }

    private fun TestGame.targetAtBeginCombat(creatureName: String) {
        passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        selectTargets(listOf(findPermanent(creatureName)!!)).error shouldBe null
        resolveStack()
    }

    init {
        test("combat damage to the player is prevented and becomes that many Mites; battle cry pumps it") {
            val game = scenario()
                .withPlayers("Ria", "Opponent")
                .withCardOnBattlefield(1, "Ria Ivor, Bane of Bladehold", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.targetAtBeginCombat("Hill Giant")
            game.shieldCount() shouldBe 1

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Ria Ivor, Bane of Bladehold" to 2, "Hill Giant" to 2)).error shouldBe null
            game.resolveStack() // battle cry: Hill Giant becomes 4/3
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("Only Ria's 3 got through; the pumped Hill Giant's 4 was prevented") {
                game.getLifeTotal(2) shouldBe 17
            }
            withClue("Four Mites — one per point of prevented damage") {
                game.findAllPermanents("Phyrexian Mite").size shouldBe 4
            }
            game.shieldCount() shouldBe 0
        }

        test("damage to a blocker is not prevented and does not spend the shield, which ends with combat") {
            val game = scenario()
                .withPlayers("Ria", "Opponent")
                .withCardOnBattlefield(1, "Ria Ivor, Bane of Bladehold", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.targetAtBeginCombat("Hill Giant")

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            withClue("The Hill Giant's damage to the blocking Bears was dealt") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            game.findAllPermanents("Phyrexian Mite").size shouldBe 0
            withClue("A blocker is not a player, so the shield is still up during combat") {
                game.shieldCount() shouldBe 1
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("\"This combat\" — the shield lapses when combat ends") {
                game.shieldCount() shouldBe 0
            }
        }

        test("a toxic creature's prevented damage gives no poison counters") {
            val game = scenario()
                .withPlayers("Ria", "Opponent")
                .withCardOnBattlefield(1, "Ria Ivor, Bane of Bladehold", summoningSickness = false)
                .withCardOnBattlefield(1, "Crawling Chorus", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.targetAtBeginCombat("Crawling Chorus")

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Crawling Chorus" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            (game.state.getEntity(game.player2Id)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0) shouldBe 0
            game.findAllPermanents("Phyrexian Mite").size shouldBe 1
        }
    }
}
