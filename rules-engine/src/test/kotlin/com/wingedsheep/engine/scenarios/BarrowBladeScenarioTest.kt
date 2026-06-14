package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Barrow-Blade — "Equipped creature gets +1/+1. Whenever equipped creature blocks or becomes
 * blocked by a creature, that creature loses all abilities until end of turn. Equip {1}."
 *
 * Exercises the ATTACHED binding for BlocksOrBecomesBlockedBy: when the equipped creature is
 * blocked, the blocker loses its abilities (here, flying).
 */
class BarrowBladeScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Barrow-Blade").activatedAbilities[0].id
    }

    init {
        test("equipped creature becoming blocked strips the blocker's abilities") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")     // 3/3 attacker (4/4 equipped)
                .withCardOnBattlefield(1, "Barrow-Blade")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(2, "Wind Drake")      // 2/2 flyer blocker
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val blade = game.findPermanent("Barrow-Blade")!!
            val drake = game.findPermanent("Wind Drake")!!

            game.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe true

            // Equip Barrow-Blade onto the Hill Giant.
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = blade,
                    abilityId = equipAbilityId,
                    targets = listOf(ChosenTarget.Permanent(giant))
                )
            ).error shouldBe null
            game.resolveStack()

            // Equipped: +1/+1.
            game.state.projectedState.getPower(giant) shouldBe 4
            game.state.projectedState.getToughness(giant) shouldBe 4

            // Attack with the Giant; opponent blocks with the Drake.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"))).error shouldBe null

            // Pass priority so the pending Barrow-Blade trigger is put on the stack, then resolve
            // it — staying in the declare-blockers step (before combat damage would kill the Drake).
            game.passPriority()
            game.resolveStack()

            // The Drake lost all abilities — including flying — until end of turn.
            game.findPermanent("Wind Drake") shouldBe drake
            game.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe false
        }
    }
}
