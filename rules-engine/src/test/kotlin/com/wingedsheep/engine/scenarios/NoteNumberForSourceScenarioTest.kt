package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.numberChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.NoteNumberForSourceEffect]: a number
 * evaluated once and written durably on the source permanent under
 * [ChoiceSlot.NOTED_NUMBER], read back through `DynamicAmount.CastChoice`. The inline card notes
 * its controller's life total as it enters (an `OnEnterRun` replacement — no trigger, no stack)
 * and again on `{T}`; the last note wins.
 */
class NoteNumberForSourceScenarioTest : ScenarioTestBase() {

    private val ledger = card("Test Life Ledger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "As this artifact enters, note your life total.\n{T}: Note your life total."

        replacementEffect(OnEnterRun(Effects.NoteNumberForSource(DynamicAmount.YourLifeTotal)))

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.NoteNumberForSource(DynamicAmount.YourLifeTotal)
        }
    }

    /** Sacrifices itself and gains life equal to its note — read as the source last existed. */
    private val spentLedger = card("Test Spent Ledger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "As this artifact enters, note your life total.\n" +
            "Sacrifice this artifact: You gain life equal to the last noted life total for this artifact."

        replacementEffect(OnEnterRun(Effects.NoteNumberForSource(DynamicAmount.YourLifeTotal)))

        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.GainLife(DynamicAmount.CastChoice(ChoiceSlot.NOTED_NUMBER))
        }
    }

    init {
        cardRegistry.register(ledger)
        cardRegistry.register(spentLedger)

        fun TestGame.noted(): Int? =
            state.getEntity(findPermanent("Test Life Ledger")!!)?.numberChoice(ChoiceSlot.NOTED_NUMBER)

        context("NoteNumberForSource") {
            test("as it enters, the life total is noted without using the stack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Life Ledger")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 17)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Life Ledger").error shouldBe null
                game.resolveStack()

                game.state.stack.isEmpty() shouldBe true
                game.noted() shouldBe 17
            }

            test("a later note replaces the earlier one with the value at resolution") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Life Ledger")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 17)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Life Ledger").error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("losing life doesn't touch the note by itself") { game.noted() shouldBe 17 }

                val ledgerId = game.findPermanent("Test Life Ledger")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ledgerId,
                        abilityId = ledger.activatedAbilities.first().id
                    )
                ).error shouldBe null
                game.resolveStack()
                game.noted() shouldBe 14
            }

            test("an ability whose source has left reads the note as the source last existed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Spent Ledger")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Spent Ledger").error shouldBe null
                game.resolveStack()
                val ledgerId = game.findPermanent("Test Spent Ledger")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ledgerId,
                        abilityId = spentLedger.activatedAbilities.first().id
                    )
                ).error shouldBe null
                game.findPermanent("Test Spent Ledger") shouldBe null
                game.resolveStack()
                withClue("the sacrificed ledger's note (6) is last-known information, not 0") {
                    game.getLifeTotal(1) shouldBe 12
                }
            }
        }
    }
}
