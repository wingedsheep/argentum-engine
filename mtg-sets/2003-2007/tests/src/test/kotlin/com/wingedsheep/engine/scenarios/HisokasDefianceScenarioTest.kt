package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hisoka's Defiance (CHK) — "Counter target Spirit or Arcane spell."
 *
 * The restriction is a subtype disjunction in the target filter, so both halves are proved — a
 * Spirit creature spell and an Arcane sorcery are each legal — and a spell that is neither is not.
 */
class HisokasDefianceScenarioTest : ScenarioTestBase() {

    private fun defiance(opponentSpell: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Hisoka's Defiance")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(2, opponentSpell)
        .withLandsOnBattlefield(2, "Forest", 3)
        .withLandsOnBattlefield(2, "Plains", 3)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hisoka's Defiance") {

            test("counters a Spirit spell") {
                val game = defiance("Kami of the Hunt")
                game.castSpell(2, "Kami of the Hunt").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Hisoka's Defiance", "Kami of the Hunt").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Kami of the Hunt") shouldBe false
                game.isInGraveyard(2, "Kami of the Hunt") shouldBe true
            }

            test("counters an Arcane spell") {
                val game = defiance("Cleanfall")
                game.castSpell(2, "Cleanfall").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Hisoka's Defiance", "Cleanfall").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Cleanfall") shouldBe true
                game.isInGraveyard(1, "Hisoka's Defiance") shouldBe true
            }

            test("a spell that is neither Spirit nor Arcane is not a legal target") {
                val game = defiance("Grizzly Bears")
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                withClue("\"Spirit or Arcane\" is a targeting restriction, so the cast must be rejected") {
                    game.castSpellTargetingStackSpell(1, "Hisoka's Defiance", "Grizzly Bears")
                        .error shouldNotBe null
                }
            }
        }
    }
}
