package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Leyline Tyrant (ZNR #147) — "When this creature dies, you may pay any amount of {R}. When you do,
 * it deals that much damage to any target."
 *
 * The amount is cost-linked and crosses the CR 603.12 reflexive round-trip from a dies trigger, and
 * the payment unit is a *red* pip: other colours of mana must not count toward it.
 */
class LeylineTyrantScenarioTest : ScenarioTestBase() {

    /** A free removal spell so the Tyrant can die without combat. */
    private val freeKill = card("Test Free Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target = TargetObject(filter = TargetFilter.Creature)
            effect = Effects.Destroy(EffectTarget.ContextTarget(0))
        }
    }

    private fun tyrantDies(mountains: Int, islands: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Leyline Tyrant")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardInHand(1, "Test Free Kill")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val tyrant = game.findPermanent("Leyline Tyrant")!!
        game.castSpell(1, "Test Free Kill", targetId = tyrant).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        cardRegistry.register(freeKill)

        test("pays only red mana, and deals that much damage to the chosen target") {
            val game = tyrantDies(mountains = 3, islands = 2)
            game.isInGraveyard(1, "Leyline Tyrant") shouldBe true

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)

            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the amount question; got ${game.getPendingDecision()}")
            withClue("the two Islands can't pay {R}; only the three Mountains count") {
                count.maxValue shouldBe 3
            }
            game.chooseNumber(3)

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            withClue("three {R} paid, three damage dealt by the dead Tyrant") {
                game.getLifeTotal(2) shouldBe 17
            }
        }

        test("declining deals no damage") {
            val game = tyrantDies(mountains = 3, islands = 0)
            game.answerYesNo(false)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.getLifeTotal(1) shouldBe 20
        }

        test("with no red mana available, nothing is asked and nothing happens") {
            val game = tyrantDies(mountains = 0, islands = 3)
            game.resolveStack()

            (game.getPendingDecision() is YesNoDecision) shouldBe false
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
