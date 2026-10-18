package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Brine Hag — {2}{U}{U} 2/2 Creature — Hag.
 *  - When this creature dies, the base power and toughness of each creature that dealt damage to
 *    it this turn become 0/2. (This effect lasts indefinitely.)
 *
 * Pinned: a creature that dealt it damage becomes 0/2 even though the Hag is already in the
 * graveyard when the trigger resolves, the change outlasts the turn, and creatures that didn't
 * damage it — or a spell that did — leave everything alone.
 */
class BrineHagScenarioTest : ScenarioTestBase() {

    init {
        context("Brine Hag") {
            test("the creature that killed it in combat becomes 0/2 indefinitely; bystanders are untouched") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "First Strike Knight", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Brine Hag")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("First Strike Knight" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Brine Hag" to listOf("First Strike Knight"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Brine Hag") shouldBe true
                val knight = game.findPermanent("First Strike Knight")!!
                val courser = game.findPermanent("Centaur Courser")!!
                withClue("first strike killed the Hag before it dealt damage, so the Knight survives as a 0/2") {
                    game.state.projectedState.getPower(knight) shouldBe 0
                    game.state.projectedState.getToughness(knight) shouldBe 2
                }
                withClue("a creature that didn't damage the Hag keeps its stats") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                    game.state.projectedState.getToughness(courser) shouldBe 3
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the change lasts indefinitely — still 0/2 on the next turn") {
                    game.state.projectedState.getPower(knight) shouldBe 0
                    game.state.projectedState.getToughness(knight) shouldBe 2
                }
            }

            test("a creature it fought to a trade dies to the change: 0/2 with 2 damage marked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Brine Hag")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Brine Hag" to listOf("Centaur Courser"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Brine Hag") shouldBe true
                withClue("the 3/3 Courser survived combat with 2 damage, then became 0/2 and died") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }
            }

            test("dying to a spell changes no creature") {
                val game = scenario()
                    .withPlayers("Caster", "Hag Owner")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Brine Hag")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Brine Hag")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Brine Hag") shouldBe true
                val courser = game.findPermanent("Centaur Courser")!!
                game.state.projectedState.getPower(courser) shouldBe 3
                game.state.projectedState.getToughness(courser) shouldBe 3
            }
        }
    }
}
