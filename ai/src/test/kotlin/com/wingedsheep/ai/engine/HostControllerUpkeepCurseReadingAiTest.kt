package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An Aura whose "at the beginning of the upkeep of enchanted permanent's controller" trigger
 * burns "that player" (`Triggers.attached.beginningOf` + `Player.TriggeringPlayer`) curses the
 * host's side, so its enchant slot reads [TargetPolarity.HARMFUL] — aimed at an opponent's
 * permanent, not our own.
 *
 * Unread, the slot was an unknown: the damage names the host's controller only as the step's
 * triggering player, never as "enchanted creature".
 */
class HostControllerUpkeepCurseReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("an upkeep burn on the enchanted permanent's controller reads harmful") {
            withClue("Feedback: 1 damage to the enchanted enchantment's controller each upkeep") {
                intents.spellTargetPolarities("Feedback") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("a burn its victim may soften by paying still reads harmful") {
            withClue("Power Leak: 2 damage to that player, less the X they pay to prevent") {
                intents.spellTargetPolarities("Power Leak") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }
    }
}
