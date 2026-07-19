package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.OjerPakpatiqDeepestEpoch
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ojer Pakpatiq, Deepest Epoch // Temple of Cyclical Time (LCI #67) — the Rebound keyword.
 *
 *  1. **Grant + exile-on-resolve** — an instant you cast from hand gains rebound: it deals its
 *     damage, then is exiled (not put in the graveyard) and a next-upkeep free recast is armed.
 *  2. **Recast at next upkeep** — at your next upkeep you may cast it from exile for free.
 *  3. **Dies → return tapped + transformed with three time counters** as Temple of Cyclical Time.
 *  4. **Back gate** — the mana ability removes a time counter; the transform is only activatable
 *     once no time counters remain.
 */
class OjerPakpatiqDeepestEpochScenarioTest : ScenarioTestBase() {

    init {
        fun timeCounters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.TIME) ?: 0

        fun exileHas(game: TestGame, player: EntityId, name: String): Boolean =
            game.state.getZone(player, Zone.EXILE).any { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

        fun graveyardHas(game: TestGame, player: EntityId, name: String): Boolean =
            game.state.getZone(player, Zone.GRAVEYARD).any { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

        context("Ojer Pakpatiq — rebound grant") {

            test("an instant cast from hand is exiled on resolution and arms a next-upkeep recast") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Pakpatiq, Deepest Epoch", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Bolt dealt its 3 damage") { game.getLifeTotal(2) shouldBe 17 }
                withClue("Bolt was exiled by rebound, not put in the graveyard") {
                    exileHas(game, game.player1Id, "Lightning Bolt") shouldBe true
                    graveyardHas(game, game.player1Id, "Lightning Bolt") shouldBe false
                }
                withClue("a next-upkeep recast is armed") {
                    game.state.delayedTriggers.size shouldBe 1
                }
            }

            test("at the caster's next upkeep the exiled instant may be recast for free") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Pakpatiq, Deepest Epoch", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(2, 20)
                    // Library padding so both players survive the draw steps across two turns.
                    .apply { repeat(6) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17

                // Advance to Player1's next upkeep (past Player2's intervening turn).
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)      // Player2 upkeep (turn 2)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // Player2 main
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)      // Player1 upkeep (turn 3) — rebound fires
                // The delayed trigger is on the stack; resolve it to surface its may-cast.
                var yesGuard = 0
                while (game.getPendingDecision() !is YesNoDecision && yesGuard++ < 10) game.resolveStack()

                withClue("the rebound recast offers a may-cast at the caster's upkeep") {
                    val yesNo = game.getPendingDecision()
                    (yesNo is YesNoDecision) shouldBe true
                    game.submitDecision(com.wingedsheep.engine.core.YesNoResponse((yesNo as YesNoDecision).id, true))
                }
                // The free cast targets a player — aim it at Player2 again.
                var guard = 0
                while (game.getPendingDecision() !is ChooseTargetsDecision && guard++ < 10) game.resolveStack()
                val td = game.getPendingDecision() as ChooseTargetsDecision
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(game.player2Id))))
                game.resolveStack()

                withClue("the free recast dealt another 3 (17 -> 14)") { game.getLifeTotal(2) shouldBe 14 }
            }
        }

        context("Ojer Pakpatiq — dies and back face") {

            test("when it dies it returns tapped as Temple of Cyclical Time with three time counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Pakpatiq, Deepest Epoch", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pakpatiq = game.findPermanent("Ojer Pakpatiq, Deepest Epoch")!!

                repeat(2) { // 6 damage kills the 4/3
                    game.castSpell(1, "Lightning Bolt", targetId = pakpatiq).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Temple of Cyclical Time") == null && guard++ < 10) game.resolveStack()

                withClue("same entity returned tapped as the back face") {
                    game.findPermanent("Temple of Cyclical Time") shouldBe pakpatiq
                    game.state.getEntity(pakpatiq)!!.get<TappedComponent>() shouldNotBe null
                }
                withClue("it entered with three time counters") {
                    timeCounters(game, pakpatiq) shouldBe 3
                }
            }

            test("the mana ability removes a time counter and the transform is gated on none remaining") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Cyclical Time")
                    .withLandsOnBattlefield(1, "Island", 3) // {2}{U} for the transform
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Cyclical Time")!!
                // Seed one time counter so we can watch the mana ability remove it.
                game.state = game.state.updateEntity(temple) {
                    it.with(CountersComponent(mapOf(CounterType.TIME to 1)))
                }

                val manaAbilityId = OjerPakpatiqDeepestEpoch.backFace!!
                    .activatedAbilities.first { it.isManaAbility }.id
                val transformAbilityId = OjerPakpatiqDeepestEpoch.backFace!!
                    .activatedAbilities.first { !it.isManaAbility }.id

                // With a time counter present, the transform is illegal.
                withClue("transform blocked while a time counter remains") {
                    game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId)
                    ).error shouldNotBe null
                }

                // Tapping for mana removes the time counter.
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = manaAbilityId)
                ).error shouldBe null
                game.resolveStack()
                withClue("the mana ability removed the time counter") { timeCounters(game, temple) shouldBe 0 }
            }
        }
    }
}
