package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Godsend (JOU #12): "Whenever equipped creature blocks or becomes blocked by one or more
 * creatures, you may exile one of those creatures. Your opponents can't cast spells with the same
 * name as a card exiled with Godsend."
 *
 * Pins the rulings: one trigger however many creatures are involved; untargeted, chosen on
 * resolution (so a protection-from-white creature can be exiled); the exiled card's name is locked
 * for opponents; declining exiles nothing.
 */
class GodsendScenarioTest : ScenarioTestBase() {

    private fun attackIntoDoubleBlock(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Godsend", "Grizzly Bears")
            .withCardOnBattlefield(2, "Ambush Viper")
            .withCardOnBattlefield(2, "Black Knight")
            .withCardInHand(2, "Ambush Viper")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(
            mapOf("Ambush Viper" to listOf("Grizzly Bears"), "Black Knight" to listOf("Grizzly Bears"))
        ).error shouldBe null
        return game
    }

    init {
        test("equipped creature gets +3/+3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Godsend", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 5
        }

        test("double block triggers once; exiling one blocker locks its name for opponents") {
            val game = attackIntoDoubleBlock()
            withClue("one trigger no matter how many creatures block (ruling)") {
                game.state.stack.size shouldBe 1
            }
            game.resolveStack()
            val viper = game.findPermanent("Ambush Viper").shouldNotBeNull()
            game.state.pendingDecision.shouldNotBeNull()
            game.selectCards(listOf(viper)).error shouldBe null
            game.resolveStack()

            withClue("the chosen blocker is exiled, the other stays") {
                game.findPermanent("Ambush Viper") shouldBe null
                game.isInExile(2, "Ambush Viper") shouldBe true
                game.findPermanent("Black Knight") shouldNotBe null
            }
            game.passPriority() // the opponent gets priority in the declare blockers step
            withClue("an opponent can't cast a spell with the exiled card's name") {
                game.castSpell(2, "Ambush Viper").error shouldNotBe null
            }
        }

        test("the exile is untargeted: a protection-from-white blocker can be chosen") {
            val game = attackIntoDoubleBlock()
            game.resolveStack()
            val knight = game.findPermanent("Black Knight").shouldNotBeNull()
            game.selectCards(listOf(knight)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Black Knight") shouldBe null
            game.isInExile(2, "Black Knight") shouldBe true
            withClue("an unrelated name stays castable") {
                game.passPriority()
                game.castSpell(2, "Ambush Viper").error shouldBe null
            }
        }

        test("declining exiles nothing") {
            val game = attackIntoDoubleBlock()
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.findPermanent("Ambush Viper") shouldNotBe null
            game.findPermanent("Black Knight") shouldNotBe null
        }

        test("triggers when the equipped creature blocks, and can exile the attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Godsend", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe null
            game.isInExile(2, "Hill Giant") shouldBe true
        }
    }
}
