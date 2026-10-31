package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Winota, Joiner of Forces (IKO #216): "Whenever a non-Human creature you control attacks, look at
 * the top six cards of your library. You may put a Human creature card from among them onto the
 * battlefield tapped and attacking. It gains indestructible until end of turn. Put the rest of the
 * cards on the bottom of your library in a random order."
 *
 * Proves what the snapshot can't: the trigger filters attackers by subtype (a Human attacker —
 * Winota included — doesn't trigger), only a Human creature card is selectable, and the
 * indestructible grant follows the tracked card that entered tapped and attacking.
 */
class WinotaJoinerOfForcesScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Winota Test Goblin",
                manaCost = ManaCost.parse("{R}"),
                subtypes = setOf(Subtype("Goblin")),
                power = 1,
                toughness = 1
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Winota Test Soldier",
                manaCost = ManaCost.parse("{1}{W}"),
                subtypes = setOf(Subtype.HUMAN, Subtype("Soldier")),
                power = 2,
                toughness = 2
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Winota Test Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        context("Winota, Joiner of Forces") {

            test("a non-Human attacker finds a Human that enters tapped, attacking and indestructible") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Winota, Joiner of Forces", summoningSickness = false)
                    .withCardOnBattlefield(1, "Winota Test Goblin", summoningSickness = false)
                    .withCardInLibrary(1, "Winota Test Soldier")
                    .withCardInLibrary(1, "Winota Test Bear")
                repeat(4) { builder.withCardInLibrary(1, "Island") }
                val game = builder
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Winota, Joiner of Forces" to 2, "Winota Test Goblin" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("only the Goblin's trigger asks for a choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val soldier = game.findCardsInLibrary(1, "Winota Test Soldier").single()
                game.selectCards(listOf(soldier)).error shouldBe null
                game.resolveStack()

                withClue("no second trigger: Winota is a Human") {
                    game.hasPendingDecision() shouldBe false
                }
                val entered = game.findPermanents("Winota Test Soldier").single()
                withClue("the Human enters tapped and attacking, with indestructible") {
                    game.state.getEntity(entered)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(entered)?.has<AttackingComponent>() shouldBe true
                    game.state.projectedState.hasKeyword(entered, Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("the other five cards, the non-Human Bear included, went to the bottom of the library") {
                    game.librarySize(1) shouldBe 5
                    game.findPermanents("Winota Test Bear").size shouldBe 0
                }
            }

            test("only Humans attacking does not trigger") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Winota, Joiner of Forces", summoningSickness = false)
                    .withCardOnBattlefield(1, "Winota Test Soldier", summoningSickness = false)
                    .withCardInLibrary(1, "Winota Test Soldier")
                repeat(5) { builder.withCardInLibrary(1, "Island") }
                val game = builder
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Winota, Joiner of Forces" to 2, "Winota Test Soldier" to 2)
                ).error shouldBe null
                game.resolveStack()

                withClue("no look, no choice") {
                    game.hasPendingDecision() shouldBe false
                    game.librarySize(1) shouldBe 6
                }
            }
        }
    }
}
