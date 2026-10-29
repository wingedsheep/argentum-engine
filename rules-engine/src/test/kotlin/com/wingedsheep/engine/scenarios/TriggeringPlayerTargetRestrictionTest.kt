package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.stack.ResolutionTargetValidator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "Choose another target player" relative to the trigger's player — a [TargetPlayer.restriction]
 * reading [Player.TriggeringPlayer] (`Not(Conditions.candidateIs(Player.TriggeringPlayer))`, The
 * Lord of Pain). The restriction is only meaningful if the triggering player is bound wherever the
 * restriction is read:
 *
 *  1. legal-target enumeration when the trigger is put on the stack (TargetFinder), and
 *  2. the CR 608.2b re-check as the ability resolves (ResolutionTargetValidator).
 *
 * With no trigger bound (an activated ability, a spell) `Player.TriggeringPlayer` resolves to no
 * one, so "is the candidate the triggering player" is false and every player stays legal.
 */
class TriggeringPlayerTargetRestrictionTest : ScenarioTestBase() {

    private val notTriggeringPlayer = Conditions.Not(Conditions.candidateIs(Player.TriggeringPlayer))
    private val anotherPlayer = TargetPlayer(restriction = notTriggeringPlayer, descriptionOverride = "another target player")

    // Whenever a player casts a spell, choose another target player. That player loses 1 life.
    private val needler = card("Another Player Test Needler") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Whenever a player casts a spell, choose another target player. That player loses 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.casts()
            val chosen = target(anotherPlayer)
            effect = Effects.LoseLife(1, chosen)
        }
    }

    init {
        cardRegistry.register(needler)

        fun twoPlayerGame(activePlayer: Int) = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Another Player Test Needler")
            .withCardInHand(activePlayer, "Grizzly Bears")
            .withLandsOnBattlefield(activePlayer, "Forest", 2)
            .withActivePlayer(activePlayer)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("an opponent's spell: the caster is excluded, so the trigger's controller is the only legal target") {
            val game = twoPlayerGame(activePlayer = 2)
            game.castSpell(2, "Grizzly Bears")
            (game.getPendingDecision() as? ChooseTargetsDecision)?.let { decision ->
                withClue("the caster (P2) is not offered") {
                    decision.legalTargets.values.flatten() shouldContainExactlyInAnyOrder listOf(game.player1Id)
                }
                game.selectTargets(listOf(game.player1Id))
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }

        test("the controller's own spell: the controller is excluded, the opponent is chosen") {
            val game = twoPlayerGame(activePlayer = 1)
            game.castSpell(1, "Grizzly Bears")
            (game.getPendingDecision() as? ChooseTargetsDecision)?.let { decision ->
                decision.legalTargets.values.flatten() shouldContainExactlyInAnyOrder listOf(game.player2Id)
                game.selectTargets(listOf(game.player2Id))
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 19
        }

        test("enumeration reads the triggering player only when one is bound") {
            val game = twoPlayerGame(activePlayer = 1)
            val source = game.findPermanent("Another Player Test Needler")!!
            val finder = services.targetFinder

            withClue("trigger bound to P2: P2 is excluded") {
                finder.findLegalTargets(
                    game.state, anotherPlayer, game.player1Id, source,
                    pipelineContext = PredicateContext(controllerId = game.player1Id, triggeringPlayerId = game.player2Id)
                ) shouldContainExactlyInAnyOrder listOf(game.player1Id)
            }
            withClue("no trigger bound: nobody is the triggering player, every player is legal") {
                finder.findLegalTargets(game.state, anotherPlayer, game.player1Id, source) shouldContainExactlyInAnyOrder
                    listOf(game.player1Id, game.player2Id)
            }
        }

        test("CR 608.2b: at resolution a target that is the triggering player is removed") {
            val game = twoPlayerGame(activePlayer = 1)
            val source = game.findPermanent("Another Player Test Needler")!!
            val validator = ResolutionTargetValidator(PredicateEvaluator(cardRegistry))
            val targets = listOf(ChosenTarget.Player(game.player2Id))

            withClue("bound to the targeted player: illegal") {
                validator.validateTargets(
                    game.state, targets, controllerId = game.player1Id, targetRequirements = listOf(anotherPlayer),
                    sourceId = source, triggeringPlayerId = game.player2Id
                ) shouldBe emptyList()
            }
            withClue("bound to someone else: still legal") {
                validator.validateTargets(
                    game.state, targets, controllerId = game.player1Id, targetRequirements = listOf(anotherPlayer),
                    sourceId = source, triggeringPlayerId = game.player1Id
                ) shouldBe targets
            }
        }
    }
}
