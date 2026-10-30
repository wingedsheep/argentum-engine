package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urza's Saga (MH2 #259).
 *
 *   I — This Saga gains "{T}: Add {C}."
 *   II — This Saga gains "{2}, {T}: Create a 0/0 Construct … +1/+1 for each artifact you control."
 *   III — Search your library for an artifact card with mana cost {0} or {1}, put it onto the
 *   battlefield, then shuffle.
 *
 * The land play gets its lore counter like any Saga, chapter I's grant leaves a working mana
 * ability, and chapter III offers exactly the {0}/{1} artifacts — not {X}{X} Chalice of the Void
 * (mana value 0) — then the Saga is sacrificed.
 */
class UrzasSagaScenarioTest : ScenarioTestBase() {

    init {
        test("played as a land, chapter I gives it {T}: Add {C}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Urza's Saga")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Urza's Saga").single()
            game.execute(PlayLand(game.player1Id, cardId)).error shouldBe null
            game.resolveStack()

            val saga = game.findPermanent("Urza's Saga")!!
            game.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
            val abilities = game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.sourceId == saga }
            withClue("chapter I's mana ability, and not yet chapter II's") {
                abilities.size shouldBe 1
                abilities.single().isManaAbility shouldBe true
            }
        }

        test("on the next turn chapter II adds the Construct ability and chapter I's stays") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Urza's Saga")
                .withCardOnBattlefield(1, "Sol Ring")
                .withLandsOnBattlefield(1, "Island", 2)
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Urza's Saga").single()
            game.execute(PlayLand(game.player1Id, cardId)).error shouldBe null
            game.resolveStack()
            // Through the opponent's turn and back to ours: the precombat-main lore counter.
            repeat(2) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            }
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val saga = game.findPermanent("Urza's Saga")!!
            game.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
            val abilities = game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.sourceId == saga }
            abilities.size shouldBe 2
            val construct = abilities.single { !it.isManaAbility }

            game.execute(construct.action).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val token = game.findPermanents("Construct Token").single()
            withClue("Sol Ring and the Construct itself: two artifacts") {
                game.state.projectedState.getPower(token) shouldBe 2
                game.state.projectedState.getToughness(token) shouldBe 2
            }
        }

        test("chapter III finds only an artifact with printed mana cost {0} or {1}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Urza's Saga")
                .withCardInLibrary(1, "Ornithopter")
                .withCardInLibrary(1, "Sol Ring")
                .withCardInLibrary(1, "Chalice of the Void")
                .withCardInLibrary(1, "Steel Overseer")
                .apply { repeat(4) { withCardInLibrary(1, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()

            val saga = game.findPermanent("Urza's Saga")!!
            game.state = game.state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, 2))
                    .with(SagaComponent(triggeredChapters = setOf(1, 2)))
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val ornithopter = game.findCardsInLibrary(1, "Ornithopter").single()
            val solRing = game.findCardsInLibrary(1, "Sol Ring").single()
            decision.options shouldContainExactlyInAnyOrder listOf(ornithopter, solRing)

            game.selectCards(listOf(solRing))
            game.resolveStack()

            game.isOnBattlefield("Sol Ring") shouldBe true
            withClue("sacrificed after its final chapter") {
                game.isInGraveyard(1, "Urza's Saga") shouldBe true
            }
        }
    }
}
