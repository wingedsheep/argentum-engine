package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Backdraft — {1}{R} Instant.
 * "Choose a player who cast one or more sorcery spells this turn. Backdraft deals damage to that
 * player equal to half the damage dealt by one of those sorcery spells this turn, rounded down."
 *
 * Pinned: the damage is the sorcery's own total over every recipient, read after it resolved;
 * you pick among that player's sorceries when they dealt different amounts; instants don't make
 * a player choosable; only players who cast a sorcery can be chosen.
 */
class BackdraftScenarioTest : ScenarioTestBase() {

    /** Player 2 is active in their main phase with [hand] and seven Mountains; Player 1 holds Backdraft. */
    private fun board(vararg hand: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Backdraft")
        .withLandsOnBattlefield(1, "Mountain", 7)
        .apply { hand.forEach { withCardInHand(2, it) } }
        .withLandsOnBattlefield(2, "Mountain", 7)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(2, "Raging Goblin")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Player 2 passes priority on an empty stack, and Player 1 casts Backdraft. */
    private fun TestGame.castBackdraft() {
        passPriority()
        castSpell(1, "Backdraft").error shouldBe null
        resolveStack()
    }

    init {
        context("Backdraft") {
            test("deals half a resolved sorcery's damage, rounded down, to its caster") {
                val game = board("Lava Axe")
                game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 15

                game.castBackdraft()

                withClue("Lava Axe dealt 5, so Backdraft deals 2 to Player 2, the only sorcery caster") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("you choose among that player's sorceries, each counting all the damage it dealt") {
                val game = board("Lava Axe", "Pyroclasm")
                game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
                game.resolveStack()
                game.castSpell(2, "Pyroclasm").error shouldBe null
                game.resolveStack()

                game.castBackdraft()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("Pyroclasm dealt 2 to each of three creatures") {
                    decision.options shouldContainExactly listOf("Lava Axe — 5 damage", "Pyroclasm — 6 damage")
                }
                game.submitDecision(OptionChosenResponse(decision.id, 1)).error shouldBe null

                game.getLifeTotal(2) shouldBe 17
            }

            test("excess damage redirected to a controller counts once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Backdraft")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Gandalf's Sanction")
                    .apply { repeat(5) { withCardInGraveyard(2, "Lightning Bolt") } }
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(2, "Gandalf's Sanction", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                withClue("2 to Grizzly Bears, the 3 excess to Player 1") {
                    game.getLifeTotal(1) shouldBe 17
                }

                game.castBackdraft()

                withClue("Gandalf's Sanction dealt 5 in all, so Backdraft deals 2") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("on the stack, the damage isn't stated before a spell is chosen") {
                val game = board("Lava Axe")
                game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
                game.resolveStack()
                game.passPriority()
                game.castSpell(1, "Backdraft").error shouldBe null

                val stackText = game.getClientState(1).cards.getValue(game.state.stack.last()).stackText!!
                withClue("the chosen spell's damage is a resolution-time number, not 0: $stackText") {
                    stackText shouldNotContain "0 damage"
                }
            }

            test("an instant's damage doesn't make its caster choosable") {
                val game = board("Lightning Bolt")
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                game.castBackdraft()

                withClue("nobody cast a sorcery, so Backdraft deals no damage") {
                    game.hasPendingDecision() shouldBe false
                    game.getLifeTotal(1) shouldBe 17
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("only players who cast a sorcery this turn are offered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lava Axe")
                    .withCardInHand(1, "Backdraft")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpellTargetingPlayer(1, "Lava Axe", 2).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Backdraft").error shouldBe null
                game.resolveStack()

                withClue("Player 1 is the only sorcery caster, so the choice lands on them without a prompt") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.getLifeTotal(1) shouldBe 18
                    game.getLifeTotal(2) shouldBe 15
                }
            }
        }
    }
}
