package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Scorpion's Sting.
 *
 * Card reference:
 * - Scorpion's Sting ({1}{B}): Instant
 *   "Target creature gets -3/-3 until end of turn."
 */
class ScorpionsStingScenarioTest : ScenarioTestBase() {

    init {
        context("Scorpion's Sting applies -3/-3 until end of turn") {

            test("reduces a 4/4 creature to 1/1 and places the spell in graveyard on resolution") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scorpion's Sting")
                    .withCardOnBattlefield(2, "Barkhide Mauler")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Barkhide Mauler")!!
                val castResult = game.castSpell(1, "Scorpion's Sting", target)
                withClue("Casting Scorpion's Sting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Scorpion's Sting should be in caster's graveyard after resolution") {
                    game.isInGraveyard(1, "Scorpion's Sting") shouldBe true
                }
                withClue("Barkhide Mauler (4/4) should survive -3/-3") {
                    game.isOnBattlefield("Barkhide Mauler") shouldBe true
                }

                val clientState = game.getClientState(1)
                val maulerInfo = clientState.cards[target]
                withClue("Barkhide Mauler should be 1/1 after -3/-3") {
                    maulerInfo shouldNotBe null
                    maulerInfo!!.power shouldBe 1
                    maulerInfo.toughness shouldBe 1
                }
            }

            test("kills a 3/3 creature via state-based actions when toughness reaches 0") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scorpion's Sting")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Hill Giant")!!
                val castResult = game.castSpell(1, "Scorpion's Sting", target)
                withClue("Casting Scorpion's Sting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should not be on the battlefield after -3/-3 reduces toughness to 0") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Hill Giant should be in its owner's graveyard") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("-3/-3 modification expires at end of turn, restoring creature to base stats") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Scorpion's Sting")
                    .withCardOnBattlefield(2, "Barkhide Mauler")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Barkhide Mauler")!!
                val castResult = game.castSpell(1, "Scorpion's Sting", target)
                withClue("Casting Scorpion's Sting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val clientStateAfterCast = game.getClientState(1)
                withClue("Barkhide Mauler should be 1/1 immediately after -3/-3 resolves") {
                    clientStateAfterCast.cards[target]?.power shouldBe 1
                    clientStateAfterCast.cards[target]?.toughness shouldBe 1
                }

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

                val clientStateAfterCleanup = game.getClientState(1)
                withClue("Barkhide Mauler should return to 4/4 after the cleanup step") {
                    clientStateAfterCleanup.cards[target]?.power shouldBe 4
                    clientStateAfterCleanup.cards[target]?.toughness shouldBe 4
                }
            }
        }
    }
}
