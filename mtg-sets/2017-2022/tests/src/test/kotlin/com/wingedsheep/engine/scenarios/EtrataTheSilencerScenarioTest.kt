package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Etrata, the Silencer (GRN #170): unblockable; on combat damage to a player it exiles target
 * creature that player controls with a hit counter, that player loses if they *own* three or more
 * exiled cards with hit counters, and Etrata is shuffled into its owner's library.
 */
class EtrataTheSilencerScenarioTest : ScenarioTestBase() {

    init {
        val etrata = "Etrata, the Silencer"

        fun builder() = scenario()
            .withPlayers("Assassin", "Target")
            .withCardOnBattlefield(1, etrata)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Forest") } }

        fun TestGame.exile(playerId: EntityId) = state.getZone(ZoneKey(playerId, Zone.EXILE))

        fun TestGame.hits(id: EntityId) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.HIT) ?: 0

        /** Give every card a player has in exile a hit counter. */
        fun TestGame.markExile(playerId: EntityId) {
            exile(playerId).forEach { id ->
                state = state.updateEntity(id) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.HIT, 1))
                }
            }
        }

        /** Attack with Etrata unblocked and target [victim] with the trigger. */
        fun TestGame.connect(victim: EntityId) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf(etrata to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            withClue("the combat-damage trigger asks for its target") {
                (getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            selectTargets(listOf(victim)).error shouldBe null
            resolveStack()
        }

        test("Etrata can't be blocked") {
            val game = builder().build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(etrata to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf(etrata))).error shouldNotBe null
        }

        test("combat damage exiles the target with a hit counter and shuffles Etrata into its owner's library") {
            val game = builder().build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val librarySize = game.librarySize(1)

            game.connect(bears)

            game.getLifeTotal(2) shouldBe 17
            game.exile(game.player2Id) shouldContain bears
            withClue("the exiled card carries a hit counter") { game.hits(bears) shouldBe 1 }
            withClue("one hit card is not enough") { game.state.gameOver shouldBe false }
            game.isOnBattlefield(etrata) shouldBe false
            game.findCardsInLibrary(1, etrata).size shouldBe 1
            game.librarySize(1) shouldBe librarySize + 1
        }

        test("the damaged player loses when they own a third exiled card with a hit counter") {
            val game = builder()
                .withCardInExile(2, "Hill Giant")
                .withCardInExile(2, "Giant Spider")
                .build()
            game.markExile(game.player2Id)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.connect(bears)

            game.hits(bears) shouldBe 1
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player1Id
        }

        test("hit-counter cards count only for their owner") {
            val game = builder()
                .withCardInExile(1, "Hill Giant")
                .withCardInExile(1, "Giant Spider")
                .withCardInExile(2, "Savannah Lions")
                .build()
            game.markExile(game.player1Id)
            game.markExile(game.player2Id)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.connect(bears)

            withClue("the target owns only two hit cards; the attacker's two don't count for them") {
                game.state.gameOver shouldBe false
            }
            game.exile(game.player2Id).count { game.hits(it) > 0 } shouldBe 2
            game.findCardsInLibrary(1, etrata).shouldNotBeNull().size shouldBe 1
        }
    }
}
