package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for `Effects.RecordValueAsEntered` — an as-enters outcome stamped on the permanent and
 * read back by a characteristic-defining P/T (Wood Elemental's shape). An inline card records the
 * number of lands its controller controls as it enters; the value must be fixed at that moment, not
 * re-evaluated when the count later changes.
 */
class RecordValueAsEnteredScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val landCounter = card("Test Land Counter") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        oracleText = "As this creature enters, note the number of lands you control. Its power and " +
            "toughness are each equal to the noted number."

        dynamicStats(DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.ValueChosenAsEntered))

        replacementEffect(
            OnEnterRun(
                Effects.RecordValueAsEntered(
                    DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land)
                )
            )
        )
    }

    init {
        cardRegistry.register(landCounter)

        context("RecordValueAsEntered") {
            test("records the amount as it enters and keeps it when the amount later changes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Land Counter")
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Land Counter").error shouldBe null
                game.resolveStack()

                val counter = game.findPermanent("Test Land Counter")!!
                withClue("three lands as it entered -> 3/3, with no prompt") {
                    game.getPendingDecision() shouldBe null
                    projector.project(game.state).getPower(counter) shouldBe 3
                    projector.project(game.state).getToughness(counter) shouldBe 3
                }

                val mountainInHand = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Mountain"
                }
                game.execute(PlayLand(game.player1Id, mountainInHand)).error shouldBe null
                withClue("a fourth land afterwards doesn't change the recorded value") {
                    projector.project(game.state).getPower(counter) shouldBe 3
                }
            }
        }
    }
}
