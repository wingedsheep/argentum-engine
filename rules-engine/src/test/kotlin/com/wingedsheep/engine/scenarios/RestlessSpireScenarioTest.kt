package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Restless Spire (WOE #260) — the Izzet creature-land.
 * "{U}{R}: Until end of turn, this land becomes a 2/1 blue and red Elemental creature with
 *  'During your turn, this creature has first strike.' It's still a land."
 * "Whenever this land attacks, scry 1."
 *
 * The animate ability is a composite: the body, plus a first-strike grant carrying
 * [com.wingedsheep.sdk.dsl.Conditions.IsYourTurn] as its `condition`, which rides along on the
 * resulting continuous effect and is re-asked on every projection. All three directions of that
 * clause are pinned here — live on your turn, dark on an opponent's turn, and dark the moment
 * another player gains control of the land — plus the intrinsic attack trigger.
 *
 * The control-change case is the one that distinguishes a genuinely continuous conditional from a
 * one-shot "is it your turn" test taken at resolution: an opponent who steals the animated land on
 * your turn has a false "your turn", so the granted clause must go dark even though the animation
 * itself (body, P/T, colors) survives the control change.
 */
class RestlessSpireScenarioTest : ScenarioTestBase() {

    private val animateAbilityId by lazy {
        // [0] {T}: add {U}, [1] {T}: add {R}, [2] {U}{R}: become a creature.
        cardRegistry.getCard("Restless Spire")!!.activatedAbilities[2].id
    }

    init {
        // An instant-speed permanent steal, so the test can hand the animated land to the opponent
        // inside the same turn it was animated. Inline test card (cf. PreventAllDamageToGroup's
        // "Test Aegis") because no printed card gains control of a permanent at instant speed.
        cardRegistry.register(
            card("Test Requisition") {
                manaCost = "{U}"
                typeLine = "Instant"
                spell {
                    val stolen = target("target permanent", Targets.Permanent)
                    effect = Effects.GainControl(stolen, Duration.Permanent)
                }
            }
        )

        fun board(activePlayer: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Restless Spire", summoningSickness = false)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            // Scry needs something to look at — the scenario builder starts with empty libraries.
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Island")
            // The opponent's instant-speed steal, for the control-change case.
            .withCardInHand(2, "Test Requisition")
            .withLandsOnBattlefield(2, "Island", 1)
            .withActivePlayer(activePlayer)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.animate() {
            val result = execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = findPermanent("Restless Spire")!!,
                    abilityId = animateAbilityId
                )
            )
            withClue("animate activation failed: ${result.error}") { result.error shouldBe null }
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
            resolveStack()
        }

        test("animating on your turn yields a 2/1 Elemental with first strike that is still a land") {
            val game = board(activePlayer = 1)
            game.animate()

            val spire = game.findPermanent("Restless Spire")!!
            val projected = game.state.projectedState
            projected.isCreature(spire) shouldBe true
            withClue("\"It's still a land\"") { projected.hasType(spire, "LAND") shouldBe true }
            projected.getPower(spire) shouldBe 2
            projected.getToughness(spire) shouldBe 1
            withClue("the quoted ability is live because it is your turn") {
                projected.hasKeyword(spire, Keyword.FIRST_STRIKE) shouldBe true
            }
        }

        test("animating during an opponent's turn yields the body without first strike") {
            val game = board(activePlayer = 2)
            game.animate()

            val spire = game.findPermanent("Restless Spire")!!
            val projected = game.state.projectedState
            withClue("the body still arrives") {
                projected.isCreature(spire) shouldBe true
                projected.getPower(spire) shouldBe 2
                projected.getToughness(spire) shouldBe 1
            }
            withClue("\"During your turn\" is false on an opponent's turn") {
                projected.hasKeyword(spire, Keyword.FIRST_STRIKE) shouldBe false
            }
        }

        test("an opponent stealing the animated land on your turn turns the granted clause off") {
            val game = board(activePlayer = 1)
            game.animate()

            val spire = game.findPermanent("Restless Spire")!!
            withClue("precondition: first strike is live while you control it on your turn") {
                game.state.projectedState.hasKeyword(spire, Keyword.FIRST_STRIKE) shouldBe true
            }

            // Still player 1's precombat main; hand priority to player 2 and let them steal it.
            game.passPriority()
            withClue("the steal should be castable at instant speed") {
                game.castSpell(2, "Test Requisition", spire).error shouldBe null
            }
            game.resolveStack()

            withClue("control actually changed") {
                game.state.projectedState.getController(spire) shouldBe game.player2Id
            }
            withClue("the animation itself survives the control change") {
                val projected = game.state.projectedState
                projected.isCreature(spire) shouldBe true
                projected.getPower(spire) shouldBe 2
                projected.getToughness(spire) shouldBe 1
            }
            withClue(
                "\"During your turn\" is read against the land's current controller, and it is not " +
                    "player 2's turn — so the granted first strike must go dark"
            ) {
                game.state.projectedState.hasKeyword(spire, Keyword.FIRST_STRIKE) shouldBe false
            }
        }

        test("attacking with the animated land scrys 1") {
            val game = board(activePlayer = 1)
            game.animate()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("declare attackers failed") {
                game.declareAttackers(mapOf("Restless Spire" to 2)).error shouldBe null
            }

            var sawScry = false
            var guard = 0
            game.resolveStack()
            while (game.getPendingDecision() != null && guard++ < 8) {
                when (game.getPendingDecision()) {
                    is SelectCardsDecision -> { sawScry = true; game.skipSelection() }
                    is ReorderLibraryDecision -> game.keepLibraryOrder()
                    else -> break
                }
                game.resolveStack()
            }

            withClue("\"Whenever this land attacks, scry 1\" fired") { sawScry shouldBe true }
        }
    }
}
