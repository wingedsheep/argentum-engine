package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain

/**
 * Game a9d25bc3 (Reality Fracture draft, 2026-11-04) turn 27: the AI, 22 to 10 up, sent a 2/2
 * Cadet token beside its 7/4 flier into eight untapped blockers led by an 11/10. The opponent
 * blocked the Cadet with the 11/10 and it died for nothing.
 *
 * The attack plan was priced at the worse of two defender replies — the predicted blocks, or no
 * blocks at all — and the Cadet improved the second (the opponent one point lower). The reply that
 * declines only the blocks costing the defender a creature, and keeps the free one on the Cadet, is
 * now priced too.
 *
 * The board is the game's, less the opponent's Mindseeker Oculus (2/1): the real margin was 0.2
 * points, and with the Oculus this rebuild values the board a hair differently and keeps the 2/2
 * home even before the fix.
 */
class HopelessAttackerStaysHomeTest : ScenarioTestBase() {

    private fun TestGame.addCounters(cardName: String, count: Int, type: CounterType = CounterType.PLUS_ONE_PLUS_ONE) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(type, count))
        }
    }

    private fun TestGame.names(ids: Collection<EntityId>): List<String> =
        ids.map { state.getEntity(it)?.get<CardComponent>()?.name ?: it.value }

    private fun turn27(): TestGame {
        var builder = scenario().withRngSeed(20261104L).withPlayers()
            .withLifeTotal(1, 22)
            .withLifeTotal(2, 10)
            .withCardOnBattlefield(1, "Proctor of Potential")
            .withCardAttachedTo(1, "Medic's Kitesail", "Proctor of Potential")
            .withCardOnBattlefield(1, "Divining Duelist")
            .withCardOnBattlefield(1, "Theorix Metamage")
            .withCardOnBattlefield(1, "Semester Foreseer")
            // The Cadet: a vanilla 2/2 token.
            .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
            .withCardOnBattlefield(1, "Eye of Jace")
            .withLandsOnBattlefield(1, "Island", 9)
            .withCardOnBattlefield(2, "Yoshimaru, Scrappy Stray")
            .withCardOnBattlefield(2, "Jiang Yanggu, Never Alone")
            .withCardOnBattlefield(2, "Ruric Thar, Magecrusher")
            .withCardOnBattlefield(2, "Inspired Tethermage")
            .withCardOnBattlefield(2, "Fatehold Chronologist")
            .withCardOnBattlefield(2, "Screeching Soulbreaker")
            .withCardOnBattlefield(2, "Variable Chaser")
            .withCardOnBattlefield(2, "Jace", isToken = true)
        listOf("Liliana the Repentant", "Massacre Girl, Most Wanted", "Swamp", "Icy Reception", "Forest")
            .forEach { builder = builder.withCardInHand(2, it) }
        repeat(4) { builder = builder.withCardInGraveyard(2, "Forest") }
        repeat(12) { i -> builder = builder.withCardOnBattlefield(2, "Forest", tapped = i < 10) }
        // Threshold: sixteen cards in the AI's graveyard make the Metamage a 3/3 flier.
        repeat(16) { builder = builder.withCardInGraveyard(1, "Island") }
        repeat(8) { builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Forest") }
        val game = builder.build()
        game.addCounters("Proctor of Potential", 3)
        game.addCounters("Inspired Tethermage", 8)
        game.addCounters("Fatehold Chronologist", 1)
        game.addCounters("Jace", 11, CounterType.LOYALTY)
        return game.advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
    }

    init {
        test("a9d25bc3 turn 27: the 2/2 does not attack into an 11/10 that blocks it for free") {
            val game = turn27()
            val attackers = (AIPlayer.create(cardRegistry, game.seatId(1), AiProfile.LIVE).chooseAction(game.state) as? DeclareAttackers)
                ?.attackers?.keys.orEmpty()
            withClue("attacked with ${game.names(attackers)}") {
                attackers shouldNotContain game.findPermanent("Grizzly Bears")!!
            }
        }
    }
}
