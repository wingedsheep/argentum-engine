package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `GrantProtectionFromChosenColorEffect` aimed at a **player** — "you gain protection from the
 * color of your choice" (CR 702.16: protection from a quality; a player can have protection).
 * The grant becomes player protection from `ProtectionScope.Color(<chosen>)`, not a floating
 * keyword, so it protects from sources of that colour only, stacks with a second grant, and ends
 * with its own duration — without ending a longer grant already on the player.
 */
class ChosenColorPlayerProtectionTest : ScenarioTestBase() {

    private val ward = card("Test Colour Ward") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(EffectTarget.Controller)) }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Test Colour Ward")
        .withCardInHand(1, "Test Colour Ward")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castWard(color: Color) {
        castSpell(1, "Test Colour Ward").error shouldBe null
        resolveStack()
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(ColorChosenResponse(decision.id, color))
    }

    private fun TestGame.protectedFrom(name: String): Boolean =
        PlayerProtectionRules.isProtectedFromSource(
            state, player1Id, findPermanent(name), player2Id, predicateEvaluator = services.predicateEvaluator
        )

    init {
        cardRegistry.register(ward)

        test("the controller gains protection from the chosen colour only") {
            val game = board()
            game.castWard(Color.RED)
            game.state.getEntity(game.player1Id)?.get<PlayerProtectionComponent>()?.scopes shouldBe
                listOf(ProtectionScope.Color(Color.RED))
            withClue("protected from a red source") { game.protectedFrom("Hill Giant") shouldBe true }
            withClue("not from a green one") { game.protectedFrom("Grizzly Bears") shouldBe false }
        }

        test("a second grant adds its colour") {
            val game = board()
            game.castWard(Color.RED)
            game.castWard(Color.GREEN)
            game.protectedFrom("Hill Giant") shouldBe true
            game.protectedFrom("Grizzly Bears") shouldBe true
        }

        test("an end-of-turn grant expires alone, leaving a longer protection in place") {
            val game = board()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(PlayerProtectionComponent.of(listOf(ProtectionScope.Color(Color.GREEN)), PlayerEffectRemoval.Permanent))
            }
            game.castWard(Color.RED)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the red grant ended with the turn") { game.protectedFrom("Hill Giant") shouldBe false }
            withClue("the permanent green protection didn't") { game.protectedFrom("Grizzly Bears") shouldBe true }
        }

        test("the grant ends at end of turn") {
            val game = board()
            game.castWard(Color.RED)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.protectedFrom("Hill Giant") shouldBe false
        }
    }
}
