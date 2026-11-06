package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.SynergyPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Sacrifice it at the beginning of the next end step" — a delayed trigger whose body sacrifices a
 * creature of ours (Hazoret's Favor, Junkyo Bell) — is a creature dying every time the card is
 * used: it feeds the death axis as a sacrifice outlet does, so a death payoff beside it is priced
 * as fuelled ([SynergyPresence]).
 */
class DelayedSacrificeSynergyAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a delayed sacrifice of our own creature feeds the death axis") {
            for (name in listOf("Hazoret's Favor", "Junkyo Bell")) {
                withClue(name) { SynergyAxes.of(intents.definition(name)!!).feeds shouldContain SynergyAxis.DEATH }
            }
        }

        test("Blood Artist beside Hazoret's Favor reads one more death to cash in") {
            val alone = scenario().withPlayers()
                .withCardOnBattlefield(1, "Blood Artist")
                .build()
            val withFavor = scenario().withPlayers()
                .withCardOnBattlefield(1, "Blood Artist")
                .withCardOnBattlefield(1, "Hazoret's Favor")
                .build()
            val term = SynergyPresence(intents)
            term.value(alone.state, alone.state.projectedState, alone.player1Id, ownSeat = true) shouldBe 0.0
            term.value(withFavor.state, withFavor.state.projectedState, withFavor.player1Id, ownSeat = true) shouldBe
                SynergyPresence.PAIR_VALUE
        }
    }
}
