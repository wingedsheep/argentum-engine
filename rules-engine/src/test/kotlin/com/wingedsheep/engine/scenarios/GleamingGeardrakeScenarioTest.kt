package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gleaming Geardrake (MKM) — {U}{R} 1/1 Artifact Creature — Drake with flying, "when this creature
 * enters, investigate", and "whenever you sacrifice an artifact, put a +1/+1 counter on this
 * creature".
 *
 * The two abilities close a loop: the Clue the Drake makes on entry is itself an artifact, so
 * cracking it for a card also grows the Drake. Nothing wires that together — the Clue's own
 * "{2}, Sacrifice this token: Draw a card" routes through the same sacrifice hook as any other,
 * which is exactly what the second test proves.
 */
class GleamingGeardrakeScenarioTest : ScenarioTestBase() {

    private val clueAbilityId = PredefinedTokens.Clue.activatedAbilities.first().id

    private fun counters(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun clue(game: TestGame) = game.state.getBattlefield()
        .firstOrNull { game.state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }

    init {
        test("entering investigates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gleaming Geardrake")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gleaming Geardrake").error shouldBe null
            game.resolveStack()

            clue(game).shouldNotBeNull()
            counters(game, "Gleaming Geardrake") shouldBe 0
        }

        test("cracking the Clue it made grows it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gleaming Geardrake")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gleaming Geardrake").error shouldBe null
            game.resolveStack()

            val clueId = clue(game).shouldNotBeNull()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = clueId, abilityId = clueAbilityId)
            ).error shouldBe null
            game.resolveStack()

            counters(game, "Gleaming Geardrake") shouldBe 1
        }
    }
}
