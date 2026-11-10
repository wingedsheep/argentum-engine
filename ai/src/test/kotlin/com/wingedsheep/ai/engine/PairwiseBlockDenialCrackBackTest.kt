package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
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
 * The pairwise form of [BlockDenialCrackBackTest]: Kozilek's Pathfinder's "{C}: Target creature
 * can't block this creature this turn" keeps one blocker off the Pathfinder only, and
 * [CombatMath.canBeBlockedBy] now reads that pairing off its floating effect, so [CrackBackStress]
 * sees the swing back it opens.
 *
 * The AI is on 5 life at its declare-attackers step with a Hill Giant and a Grizzly Bears; the
 * opponent's 5/5 Pathfinder is tapped from its own attack, so the Giant's 3 damage is free. Home
 * alone, the Bears chumps the Pathfinder on the swing back — unless the opponent's Wastes pays
 * {C} to keep the Bears off it, and 5 damage is lethal. With both home, one is left to chump.
 */
class PairwiseBlockDenialCrackBackTest : ScenarioTestBase() {

    private fun board(wastes: Boolean): TestGame {
        var builder = scenario().withRngSeed(20261110L).withPlayers()
            .withLifeTotal(1, 5)
            .withLifeTotal(2, 20)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Kozilek's Pathfinder", tapped = true)
        if (wastes) builder = builder.withLandsOnBattlefield(2, "Wastes", 1)
        repeat(6) { builder = builder.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain") }
        return builder.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
    }

    private fun TestGame.attackersChosenBy(profile: AiProfile): Set<EntityId> =
        (AIPlayer.create(cardRegistry, seatId(1), profile).chooseAction(state) as? DeclareAttackers)
            ?.attackers?.keys.orEmpty()

    private fun TestGame.names(ids: Collection<EntityId>): List<String> =
        ids.map { state.getEntity(it)?.get<CardComponent>()?.name ?: it.value }

    init {
        test("a pairwise can't-block is a block denial") {
            val ability = IntentCatalog.of(cardRegistry).definition("Kozilek's Pathfinder")!!
                .script.activatedAbilities.single()
            CombatReach.of(ability.effect) shouldContain CombatReach.BLOCK_DENIAL
        }

        test("with no colorless mana to pay, the free attack goes in: the Bears home chumps") {
            val game = board(wastes = false)
            val attackers = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(attackers)}") {
                attackers shouldContain game.findPermanent("Hill Giant")!!
            }
        }

        test("a {C} that keeps the lone blocker off the Pathfinder keeps both blockers home") {
            val game = board(wastes = true)
            val attackers = game.attackersChosenBy(AiProfile.LIVE)
            withClue("attacked with ${game.names(attackers)}") {
                attackers shouldNotContain game.findPermanent("Hill Giant")!!
                attackers shouldNotContain game.findPermanent("Grizzly Bears")!!
            }
        }
    }
}
