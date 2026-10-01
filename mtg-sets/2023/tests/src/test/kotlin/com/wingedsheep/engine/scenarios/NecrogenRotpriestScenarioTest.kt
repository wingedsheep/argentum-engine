package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.NecrogenRotpriest
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Necrogen Rotpriest (ONE #212) — {2}{B}{G} 1/5 Creature — Phyrexian Zombie Cleric
 *   Toxic 2
 *   Whenever a creature you control with toxic deals combat damage to a player, that player gets
 *   an additional poison counter.
 *   {1}{B}{G}: Target creature you control with toxic gains deathtouch until end of turn.
 */
class NecrogenRotpriestScenarioTest : ScenarioTestBase() {

    private fun TestGame.poison(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    init {
        test("each toxic creature you control that deals combat damage to a player adds one more poison") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Necrogen Rotpriest")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Necrogen Rotpriest" to 2, "Blightbelly Rat" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Rotpriest toxic 2 + 1 extra, Rat toxic 1 + 1 extra, Bears (no toxic) nothing.
            game.poison(game.player2Id) shouldBe 5
            game.getLifeTotal(2) shouldBe 15
        }

        test("activated ability grants deathtouch to a creature you control with toxic only") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Necrogen Rotpriest")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val priest = game.findPermanent("Necrogen Rotpriest")!!
            val rat = game.findPermanent("Blightbelly Rat")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = NecrogenRotpriest.activatedAbilities.single().id

            game.execute(
                ActivateAbility(game.player1Id, priest, abilityId, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null

            game.execute(
                ActivateAbility(game.player1Id, priest, abilityId, targets = listOf(ChosenTarget.Permanent(rat)))
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(rat, Keyword.DEATHTOUCH) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
        }
    }
}
