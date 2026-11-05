package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A can't-block activation on the opponent's board is in the crack-back ([CrackBackStress] reading
 * [com.wingedsheep.ai.engine.knowledge.CombatReach.BLOCK_DENIAL]).
 *
 * The AI is on 3 life at its declare-attackers step with a Hill Giant and a Grizzly Bears; the
 * opponent's Craw Wurm is tapped from its own attack, so the Giant's 3 damage is free. Home, the
 * Bears chumps the Wurm on the swing back — unless the opponent's Alchemist's Vial ("{1}, {T},
 * Sacrifice: target creature can't attack or block this turn") takes the Bears out of the block,
 * and the 6/4 walks in for lethal. With both creatures home, one is left to chump.
 */
class BlockDenialCrackBackTest : ScenarioTestBase() {

    private fun board(vial: Boolean): TestGame {
        var builder = scenario().withRngSeed(20261105L).withPlayers()
            .withLifeTotal(1, 3)
            .withLifeTotal(2, 20)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Craw Wurm", tapped = true)
            .withLandsOnBattlefield(2, "Mountain", 2)
        if (vial) builder = builder.withCardOnBattlefield(2, "Alchemist's Vial")
        repeat(6) { builder = builder.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain") }
        return builder.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
    }

    private fun TestGame.attackersChosenBy(profile: AiProfile): Set<EntityId> =
        (AIPlayer.create(cardRegistry, seatId(1), profile).chooseAction(state) as? DeclareAttackers)
            ?.attackers?.keys.orEmpty()

    private fun TestGame.names(ids: Collection<EntityId>): List<String> =
        ids.map { state.getEntity(it)?.get<CardComponent>()?.name ?: it.value }

    init {
        test("without the Vial the free attack goes in: the Bears home chumps the Wurm") {
            val game = board(vial = false)
            val attackers = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(attackers)}") {
                attackers shouldContain game.findPermanent("Hill Giant")!!
            }
        }

        test("a can't-block activation on their board keeps both blockers home") {
            val game = board(vial = true)
            val giant = game.findPermanent("Hill Giant")!!

            val before = game.attackersChosenBy(
                AiProfile.LIVE.copy(id = "live-without-crackback-stress", stressCrackBack = false),
            )
            withClue("reproduction: attacked with ${game.names(before)}") { before shouldContain giant }

            val attackers = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(attackers)}") { attackers shouldNotContain giant }
        }
    }
}
