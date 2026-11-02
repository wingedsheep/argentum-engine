package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Suncleanser (M19 #39) — {1}{W} Creature — Human Cleric 1/4.
 *
 *   When this creature enters, choose one —
 *   • Remove all counters from target creature. It can't have counters put on it for as long as
 *     this creature remains on the battlefield.
 *   • Target opponent loses all counters. That player can't get counters for as long as this
 *     creature remains on the battlefield.
 */
class SuncleanserScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    /** Suncleanser leaves the battlefield (to its owner's graveyard). */
    private fun TestGame.removeSuncleanser() {
        val id = findPermanent("Suncleanser")!!
        state = state.moveToZone(id, ZoneKey(player1Id, Zone.BATTLEFIELD), ZoneKey(player1Id, Zone.GRAVEYARD))
        checkStateBasedActions()
    }

    private fun TestGame.castDruidOnto(creature: EntityId) {
        castSpell(1, "Pollenbright Druid").error shouldBe null
        resolveStack()
        chooseMode(0)
        selectTargets(listOf(creature)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.castIchorRats() {
        castSpell(1, "Ichor Rats").error shouldBe null
        resolveStack()
        resolveStack()
    }

    init {
        test("creature mode: removes every counter, and no counter goes on it until Suncleanser leaves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Suncleanser")
                .withCardsInHand(1, "Pollenbright Druid", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 2)
            seed(game, giant, CounterType.STUN, 1)

            game.castSpell(1, "Suncleanser").error shouldBe null
            game.resolveStack()
            game.chooseMode(0)
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("all counters, of every kind, are removed") {
                count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                count(game, giant, CounterType.STUN) shouldBe 0
            }

            game.castDruidOnto(giant)
            withClue("it can't have counters put on it while Suncleanser remains") {
                count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }

            game.removeSuncleanser()
            game.castDruidOnto(giant)
            withClue("once Suncleanser has left, counters go on it again") {
                count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }

        test("player mode: the opponent loses all counters and gets none until Suncleanser leaves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Suncleanser")
                .withCardsInHand(1, "Ichor Rats", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val opponent = game.player2Id
            seed(game, opponent, CounterType.POISON, 3)
            seed(game, opponent, CounterType.ENERGY, 2)

            game.castSpell(1, "Suncleanser").error shouldBe null
            game.resolveStack()
            game.chooseMode(1)
            // The lone opponent is the only legal target, so it's chosen without a prompt.
            if (game.hasPendingDecision()) game.selectTargets(listOf(opponent)).error shouldBe null
            game.resolveStack()

            withClue("the opponent loses all counters") {
                count(game, opponent, CounterType.POISON) shouldBe 0
                count(game, opponent, CounterType.ENERGY) shouldBe 0
            }

            game.castIchorRats()
            withClue("each player gets a poison counter — except the locked opponent") {
                count(game, game.player1Id, CounterType.POISON) shouldBe 1
                count(game, opponent, CounterType.POISON) shouldBe 0
            }

            game.removeSuncleanser()
            game.castIchorRats()
            withClue("once Suncleanser has left, the opponent can get counters again") {
                count(game, opponent, CounterType.POISON) shouldBe 1
                count(game, game.player1Id, CounterType.POISON) shouldBe 2
            }
        }
    }
}
