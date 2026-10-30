package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.OptionType
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Feature test for `ChooseOptionEffect.recordOnSource` — a choice made by a resolving ability,
 * recorded durably on the source permanent under the option type's slot — and for reading a
 * recorded card name back in a delayed "when you cast a spell with the chosen name for the first
 * time this turn" trigger (`castsNth(1, namedFromChosenComponent())`), the shape Medomai's
 * Prophecy chapters II and III use.
 *
 * The inline card's first ability names a card, its second chooses a color, and its third creates
 * the delayed draw trigger.
 */
class ChooseOptionRecordOnSourceScenarioTest : ScenarioTestBase() {

    private val keeper = card("Test Name Keeper") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{1}: Choose a card name.\n{2}: Choose a color.\n" +
            "{3}: When you cast a spell with the chosen name for the first time this turn, draw a card."

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.ChooseCardName(recordOnSource = true)
        }
        activatedAbility {
            cost = Costs.Mana("{2}")
            effect = ChooseOptionEffect(OptionType.COLOR, recordOnSource = true)
        }
        activatedAbility {
            cost = Costs.Mana("{3}")
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.you.castsNth(1, GameObjectFilter.Any.namedFromChosenComponent()),
                effect = Effects.DrawCards(1),
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
        }
    }

    /** Names a card without recording it — the pipeline-only default. */
    private val forgetful = card("Test Forgetful Namer") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{1}: Choose a card name."

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.ChooseCardName()
        }
    }

    init {
        cardRegistry.register(keeper)
        cardRegistry.register(forgetful)

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Name Keeper")
            .withLandsOnBattlefield(1, "Mountain", 12)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(6) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }

        fun TestGame.activate(def: CardDefinition, index: Int) {
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = findPermanent(def.name)!!,
                    abilityId = def.activatedAbilities[index].id
                )
            ).error shouldBe null
            resolveStack()
        }

        fun TestGame.choose(option: String) {
            val decision = getPendingDecision() as ChooseOptionDecision
            submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(option))).error shouldBe null
            resolveStack()
        }

        fun TestGame.recordedName(def: CardDefinition = keeper): String? =
            state.getEntity(findPermanent(def.name)!!)?.chosenCardName()

        fun TestGame.nameCard(option: String) {
            activate(keeper, 0)
            choose(option)
        }

        fun TestGame.boltOpponent() {
            castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            resolveStack()
        }

        context("recording the choice on the source") {
            test("a card name is recorded on the source permanent, and a later choice replaces it") {
                val game = builder().build()
                game.nameCard("Lightning Bolt")
                game.recordedName() shouldBe "Lightning Bolt"

                game.nameCard("Shock")
                game.recordedName() shouldBe "Shock"
            }

            test("a color is recorded as a color choice") {
                val game = builder().build()
                game.activate(keeper, 1)
                game.choose("Blue")
                game.state.getEntity(game.findPermanent("Test Name Keeper")!!)?.chosenColor() shouldBe Color.BLUE
            }

            test("without recordOnSource the name lives only in the pipeline") {
                val game = builder().withCardOnBattlefield(1, "Test Forgetful Namer").build()
                game.activate(forgetful, 0)
                game.choose("Lightning Bolt")
                game.recordedName(forgetful).shouldBeNull()
            }
        }

        context("a delayed first-cast trigger keyed to the recorded name") {
            test("the first spell with the chosen name draws; a second one this turn doesn't") {
                val game = builder()
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .build()
                game.nameCard("Lightning Bolt")
                game.activate(keeper, 2)

                val before = game.handSize(1)
                game.boltOpponent()
                withClue("cast one Bolt (-1), drew one (+1)") { game.handSize(1) shouldBe before }
                game.boltOpponent()
                withClue("the second Bolt is not the first this turn") { game.handSize(1) shouldBe before - 1 }
            }

            test("a spell with another name neither triggers nor uses the trigger up") {
                val game = builder()
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Lightning Bolt")
                    .build()
                game.nameCard("Lightning Bolt")
                game.activate(keeper, 2)

                val before = game.handSize(1)
                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before - 1
                game.boltOpponent()
                game.handSize(1) shouldBe before - 1
            }

            test("a spell with that name cast earlier this turn means a later one isn't the first") {
                val game = builder()
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .build()
                game.nameCard("Lightning Bolt")
                game.boltOpponent()
                game.activate(keeper, 2)

                val before = game.handSize(1)
                game.boltOpponent()
                game.handSize(1) shouldBe before - 1
            }

            test("the name is frozen as the trigger is created") {
                val game = builder()
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Shock")
                    .build()
                game.nameCard("Lightning Bolt")
                game.activate(keeper, 2)
                game.nameCard("Shock")

                val before = game.handSize(1)
                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()
                withClue("renaming the source afterwards doesn't retarget the trigger") {
                    game.handSize(1) shouldBe before - 1
                }
                game.boltOpponent()
                game.handSize(1) shouldBe before - 1
            }

            test("with no name chosen the trigger can't trigger") {
                val game = builder().withCardInHand(1, "Lightning Bolt").build()
                game.activate(keeper, 2)

                val before = game.handSize(1)
                game.boltOpponent()
                game.handSize(1) shouldBe before - 1
            }

            test("the trigger ends with the turn") {
                val game = builder().withCardInHand(1, "Lightning Bolt").build()
                game.nameCard("Lightning Bolt")
                game.activate(keeper, 2)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
                game.passPriority()

                val before = game.handSize(1)
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before - 1
            }
        }
    }
}
