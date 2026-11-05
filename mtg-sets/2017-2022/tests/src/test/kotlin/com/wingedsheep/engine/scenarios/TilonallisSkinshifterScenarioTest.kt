package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tilonalli's Skinshifter (XLN #170) — "Whenever this creature attacks, it becomes a copy of another
 * target nonlegendary attacking creature until end of turn."
 *
 * The copy swaps the card's copiable values mid-combat; this proves the Skinshifter stays an attacker
 * after becoming the copy and reverts to itself once the turn ends.
 */
class TilonallisSkinshifterScenarioTest : ScenarioTestBase() {
    init {
        test("becomes a copy of a fellow attacker, keeps attacking, and reverts at end of turn") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Tilonalli's Skinshifter")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val skinshifter = game.findPermanent("Tilonalli's Skinshifter")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.declareAttackers(mapOf("Tilonalli's Skinshifter" to 2, "Hill Giant" to 2)).error shouldBe null
            game.getPendingDecision().shouldNotBeNull()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("Skinshifter is now a Hill Giant copy (3/3)") {
                game.state.getEntity(skinshifter)?.get<CardComponent>()?.name shouldBe "Hill Giant"
                game.state.projectedState.getPower(skinshifter) shouldBe 3
                game.state.projectedState.getToughness(skinshifter) shouldBe 3
            }
            withClue("the copy is still attacking") {
                game.state.getEntity(skinshifter)!!.has<AttackingComponent>() shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            withClue("the copy effect ends at end of turn") {
                game.state.getEntity(skinshifter)?.get<CardComponent>()?.name shouldBe "Tilonalli's Skinshifter"
                game.state.projectedState.getPower(skinshifter) shouldBe 0
                game.state.projectedState.getToughness(skinshifter) shouldBe 1
            }
        }
    }
}
