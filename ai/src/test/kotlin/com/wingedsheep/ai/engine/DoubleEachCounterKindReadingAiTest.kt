package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Double the number of each kind of counter on target artifact, creature, or land" (Vorel of the
 * Hull Clade; Zimone, Paradox Sculptor over up to two of ours) — `Effects.DoubleAllCounters`. Every
 * kind at once is a targeted proliferate many times over: the slot reads FLEXIBLE for the simulation
 * to weigh each holder's counters, it needs no intent tag, and it cashes in the +1/+1 counters the
 * deck puts down.
 */
class DoubleEachCounterKindReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun TestGame.withCounters(cardName: String, type: CounterType, count: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(type, count))
        }
    }

    init {
        test("Vorel and Zimone read fully and reward the +1/+1 counter axis") {
            listOf("Vorel of the Hull Clade", "Zimone, Paradox Sculptor").forEach { name ->
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.readFully shouldBe true }
                withClue(name) { SynergyAxes.of(card(name)).rewards shouldContain SynergyAxis.PLUS_COUNTERS }
            }
        }

        test("Vorel doubles the counters on our own creature, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Vorel of the Hull Clade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.withCounters("Grizzly Bears", CounterType.PLUS_ONE_PLUS_ONE, 2)
            game.withCounters("Hill Giant", CounterType.PLUS_ONE_PLUS_ONE, 2)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            action.shouldBeInstanceOf<ActivateAbility>()
            action.sourceId shouldBe game.findPermanent("Vorel of the Hull Clade")
            withClue("the doubled +1/+1 counters belong on our Bears") {
                (action.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.findPermanent("Grizzly Bears")
            }
        }
    }
}
