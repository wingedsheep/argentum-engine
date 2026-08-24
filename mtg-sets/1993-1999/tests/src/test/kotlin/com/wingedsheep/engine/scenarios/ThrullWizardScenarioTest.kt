package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fem.cards.ThrullWizard
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Thrull Wizard (Fallen Empires).
 *
 * Oracle: "{1}{B}: Counter target black spell unless that spell's controller pays {B} or {3}."
 *
 * The half that is easy to get wrong is *where* the target lives. A [com.wingedsheep.sdk.scripting.filters.unified.TargetFilter]
 * defaults to `Zone.BATTLEFIELD`, so a colour-filtered filter built from scratch looks for a black
 * *permanent* and the ability can never be activated at all — the stack has to be asked for
 * explicitly. Hence the first test: a black spell on the stack is a legal target.
 *
 * The rest is Erosion's shape pointed at the stack: the payer is the *targeted spell's* controller,
 * not the Wizard's, and the price is a genuine choice — {B} and {3} neither subsume each other.
 */
class ThrullWizardScenarioTest : ScenarioTestBase() {

    private val abilityId = ThrullWizard.activatedAbilities.first().id

    init {
        context("Thrull Wizard — {1}{B}: counter target black spell unless its controller pays") {

            /**
             * Player 2 is the active player with a black creature spell already on the stack and
             * three Swamps left untapped, so both legs of the "{B} or {3}" choice are payable.
             * Priority has been passed back to player 1, holding an untapped Thrull Wizard.
             */
            fun blackSpellOnTheStack(): Pair<TestGame, EntityId> {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thrull Wizard", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)   // pays the {1}{B} activation
                    .withCardInHand(2, "Basal Thrull")
                    .withLandsOnBattlefield(2, "Swamp", 5)   // {B}{B} to cast, three left over
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Basal Thrull").error shouldBe null
                game.autoPayIfAsked()
                val spell = game.spellOnStack("Basal Thrull")
                game.passPriority().error shouldBe null
                return game to spell
            }

            test("a black spell on the stack is a legal target") {
                val (game, spell) = blackSpellOnTheStack()

                val activate = game.activateWizard(spell)
                withClue("the ability targets the stack, not the battlefield: ${activate.error}") {
                    activate.error shouldBe null
                }
                withClue("the ability should be waiting on the stack above its target") {
                    game.state.stack.size shouldBe 2
                }
            }

            test("declining the payment counters the spell") {
                val (game, spell) = blackSpellOnTheStack()
                game.activateWizard(spell).error shouldBe null
                game.autoPayIfAsked()
                game.resolveStack()

                val choice = game.getPendingDecision() as ChooseOptionDecision
                withClue("the spell's controller pays, not the Wizard's") {
                    choice.playerId shouldBe game.player2Id
                }
                // The last option is always the consequence — "counter that spell".
                game.submitDecision(OptionChosenResponse(choice.id, choice.options.size - 1))
                game.resolveStack()

                game.isOnBattlefield("Basal Thrull") shouldBe false
                game.isInGraveyard(2, "Basal Thrull") shouldBe true
            }

            test("paying {B} saves the spell") {
                val (game, spell) = blackSpellOnTheStack()
                game.activateWizard(spell).error shouldBe null
                game.autoPayIfAsked()
                game.resolveStack()

                val choice = game.getPendingDecision() as ChooseOptionDecision
                withClue("both legs of the choice are payable with three Swamps up: ${choice.options}") {
                    choice.options.size shouldBe 3
                }
                val payB = choice.options.indexOfFirst { it.contains("{B}") }
                payB shouldNotBe -1
                game.submitDecision(OptionChosenResponse(choice.id, payB))

                // Picking a leg only selects it; the engine then confirms the payment itself.
                val confirm = game.getPendingDecision() as YesNoDecision
                withClue("the confirmation is asked of the payer, in the payer's words") {
                    confirm.playerId shouldBe game.player2Id
                    confirm.prompt shouldBe "Pay {B} or counter that spell?"
                }
                game.answerYesNo(true)
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("the spell was paid for and resolved") {
                    game.isOnBattlefield("Basal Thrull") shouldBe true
                }
            }

            test("a nonblack spell can't be targeted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thrull Wizard", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.autoPayIfAsked()
                val bolt = game.spellOnStack("Lightning Bolt")
                game.passPriority().error shouldBe null

                withClue("red is not black — the ability has no legal target here") {
                    game.activateWizard(bolt).error shouldNotBe null
                }
            }
        }
    }

    /** The single stack object with the given card name. */
    private fun TestGame.spellOnStack(name: String): EntityId =
        state.stack.single { id -> state.getEntity(id)?.get<CardComponent>()?.name == name }

    /** Activate Thrull Wizard's counter ability at [spell], auto-paying the {1}{B}. */
    private fun TestGame.activateWizard(spell: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Thrull Wizard")!!,
            abilityId = abilityId,
            targets = listOf(ChosenTarget.Spell(spell))
        )
    )

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        while (getPendingDecision() is SelectManaSourcesDecision) {
            submitManaSourcesAutoPay()
        }
    }
}
