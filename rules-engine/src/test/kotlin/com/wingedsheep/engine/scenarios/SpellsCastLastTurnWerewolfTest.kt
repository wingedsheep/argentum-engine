package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmount.SpellsCastLastTurn` — the original Innistrad werewolves' transform clauses.
 *
 * | Wording | Rule it pins | Covered by |
 * |---|---|---|
 * | "if no spells were cast last turn" | counts *every* player's spells, an opponent's instant on your turn included | "an opponent's instant last turn keeps the front face" |
 * | same | a turn with no spells at all transforms it | "no spells last turn transforms the front face" |
 * | "if a player cast two or more spells last turn" | one player's count, not the table's sum | "one spell from each player doesn't transform the back face" |
 * | same | two spells by the non-active player count | "two spells by the non-active player transform the back face" |
 * | day/night (CR 732.2a) | still reads only the previous turn's active player | "day/night ignores the opponent's spells" |
 */
class SpellsCastLastTurnWerewolfTest : ScenarioTestBase() {

    private val werewolf: CardDefinition = CardDefinition.doubleFacedCreature(
        frontFace = card("Test Old Werewolf") {
            manaCost = "{1}{R}"
            typeLine = "Creature — Human Werewolf"
            power = 2
            toughness = 2
            triggeredAbility {
                trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
                interveningIf = Conditions.CompareAmounts(
                    DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
                )
                effect = Effects.Transform(EffectTarget.Self)
            }
        },
        backFace = card("Test Old Werewolf Night") {
            manaCost = ""
            colorIndicator = "R"
            typeLine = "Creature — Werewolf"
            power = 3
            toughness = 3
            triggeredAbility {
                trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
                interveningIf = Conditions.CompareAmounts(
                    DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
                )
                effect = Effects.Transform(EffectTarget.Self)
            }
        },
    )

    private val cantrip = card("Test Free Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1, EffectTarget.Controller) }
    }

    init {
        listOf(werewolf, cantrip).forEach { cardRegistry.register(it) }

        fun ScenarioTestBase.TestGame.nameOf(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

        /** Player 1's main phase, the werewolf on player 1's side — naming the back face puts it in transformed. */
        fun game(p1Instants: Int = 0, p2Instants: Int = 0, back: Boolean = false): ScenarioTestBase.TestGame {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, if (back) "Test Old Werewolf Night" else "Test Old Werewolf")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(p1Instants) { builder.withCardInHand(1, "Test Free Instant") }
            repeat(p2Instants) { builder.withCardInHand(2, "Test Free Instant") }
            return builder.build()
        }

        /** Each player casts their instants during player 1's turn, then the game moves to player 2's upkeep. */
        fun ScenarioTestBase.TestGame.castAndAdvance(p1Instants: Int, p2Instants: Int) {
            repeat(p1Instants) {
                castSpell(1, "Test Free Instant").error shouldBe null
                resolveStack()
            }
            repeat(p2Instants) {
                // Player 1 passes priority on an empty stack, handing it to player 2.
                passPriority()
                castSpell(2, "Test Free Instant").error shouldBe null
                resolveStack()
            }
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            resolveStack()
        }

        context("\"if no spells were cast last turn\"") {

            test("no spells last turn transforms the front face") {
                val g = game()
                val wolf = g.findPermanent("Test Old Werewolf")!!
                g.castAndAdvance(0, 0)
                g.state.activePlayerId shouldBe g.player2Id
                g.nameOf(wolf) shouldBe "Test Old Werewolf Night"
            }

            test("an opponent's instant last turn keeps the front face") {
                val g = game(p2Instants = 1)
                val wolf = g.findPermanent("Test Old Werewolf")!!
                g.castAndAdvance(0, 1)
                withClue("the snapshot holds the non-active player's spell") {
                    g.state.previousTurnSpellCountsByPlayer[g.player2Id] shouldBe 1
                }
                withClue("a spell was cast last turn, by anyone, so the werewolf doesn't transform") {
                    g.nameOf(wolf) shouldBe "Test Old Werewolf"
                }
            }
        }

        context("\"if a player cast two or more spells last turn\"") {

            test("one spell from each player doesn't transform the back face") {
                val g = game(p1Instants = 1, p2Instants = 1, back = true)
                val wolf = g.findPermanent("Test Old Werewolf Night")!!
                g.castAndAdvance(1, 1)
                withClue("two spells were cast in total, but no single player cast two") {
                    g.nameOf(wolf) shouldBe "Test Old Werewolf Night"
                }
            }

            test("two spells by the non-active player transform the back face") {
                val g = game(p2Instants = 2, back = true)
                val wolf = g.findPermanent("Test Old Werewolf Night")!!
                g.castAndAdvance(0, 2)
                g.nameOf(wolf) shouldBe "Test Old Werewolf"
            }
        }

        context("day/night is unchanged") {

            test("day/night ignores the opponent's spells (CR 732.2a reads the active player)") {
                val g = game()
                g.state = g.state.copy(
                    dayNight = DayNight.DAY,
                    previousTurnActiveTeamSpellCounts = mapOf(g.player1Id to 0),
                    previousTurnSpellCountsByPlayer = mapOf(g.player2Id to 1),
                )
                val (after, _) = DayNightService.checkUntapStepDesignation(g.state, cardRegistry)
                after.dayNight shouldBe DayNight.NIGHT
            }
        }
    }
}
