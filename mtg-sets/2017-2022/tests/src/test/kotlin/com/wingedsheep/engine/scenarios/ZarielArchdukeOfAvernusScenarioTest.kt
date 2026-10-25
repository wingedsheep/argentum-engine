package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zariel, Archduke of Avernus (AFR #172) — the −6 emblem: "At the end of the first combat phase on
 * your turn, untap target creature you control. After this phase, there is an additional combat
 * phase."
 *
 * Proves the end-of-combat trigger fires at the end of the first combat only — it untaps the
 * attacker and queues exactly one extra combat, and the extra combat's end doesn't trigger again.
 */
class ZarielArchdukeOfAvernusScenarioTest : ScenarioTestBase() {

    private val zarielName = "Zariel, Archduke of Avernus"

    init {
        context("Zariel, Archduke of Avernus emblem") {

            test("untaps the attacker at the end of the first combat and adds exactly one more combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, zarielName)
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zariel = game.findPermanent(zarielName)!!
                val courser = game.findPermanent("Centaur Courser")!!
                game.state = game.state.updateEntity(zariel) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
                }
                fun queued() = game.state.getEntity(game.state.activePlayerId!!)
                    ?.get<AdditionalPhasesComponent>()?.phases.orEmpty()
                fun isTapped() = game.state.getEntity(courser)!!.has<TappedComponent>()

                val ultimate = cardRegistry.getCard(zarielName)!!.script.activatedAbilities[2]
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = zariel, abilityId = ultimate.id)
                ).error shouldBe null
                game.resolveStack()
                withClue("Zariel went to 0 loyalty and died; the emblem stays") {
                    game.findPermanent(zarielName) shouldBe null
                }

                // First combat.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("the emblem triggers at the end of the first combat and asks for a target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("the attacker untaps and one additional combat phase is queued") {
                    isTapped() shouldBe false
                    queued().map { it.kind } shouldBe listOf(ExtraPhaseKind.COMBAT)
                }

                // Additional combat: the emblem doesn't trigger at its end.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("no trigger at the end of the additional combat") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                    isTapped() shouldBe true
                    queued().size shouldBe 0
                }
                game.getLifeTotal(2) shouldBe 14
            }
        }
    }
}
