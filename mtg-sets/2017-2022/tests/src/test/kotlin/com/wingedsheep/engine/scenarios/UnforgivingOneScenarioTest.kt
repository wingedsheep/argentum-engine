package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unforgiving One (NEO #127) — {2}{B} Creature — Spirit, 2/3, menace.
 *
 * "Whenever this creature attacks, return target creature card with mana value X or less from your
 *  graveyard to the battlefield, where X is the number of modified creatures you control."
 *
 * Covers that X counts modified creatures you control (Unforgiving One included) when the target is
 * chosen, and that X is re-read on resolution (ruling 2022-02-18): losing a modification before the
 * trigger resolves makes the target illegal and the card stays in the graveyard.
 */
class UnforgivingOneScenarioTest : ScenarioTestBase() {

    private val oneDrop = card("Test One-Drop") {
        manaCost = "{B}"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 1
    }

    private val twoDrop = card("Test Two-Drop") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(oneDrop)
        cardRegistry.register(twoDrop)

        fun board() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Unforgiving One")
            .withCardInGraveyard(1, "Test One-Drop")
            .withCardInGraveyard(1, "Test Two-Drop")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.setCounters(name: String, count: Int) {
            val id = findPermanent(name)!!
            state = state.updateEntity(id) {
                it.with(CountersComponent(if (count > 0) mapOf(CounterType.PLUS_ONE_PLUS_ONE to count) else emptyMap()))
            }
        }

        context("Unforgiving One") {

            test("one modified creature (itself) lets it return a mana value 1 creature card, not a 2") {
                val game = board()
                game.setCounters("Unforgiving One", 1)
                val oneDropCard = game.findCardsInGraveyard(1, "Test One-Drop").single()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Unforgiving One" to 2)).error shouldBe null

                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("X = 1: only the mana value 1 card is a legal target") {
                    decision.legalTargets.values.flatten().toSet() shouldBe setOf(oneDropCard)
                }
                game.selectTargets(listOf(oneDropCard)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Test One-Drop") shouldBe oneDropCard
                game.isInGraveyard(1, "Test Two-Drop") shouldBe true
            }

            test("X is re-read on resolution: losing the modification leaves the target in the graveyard") {
                val game = board()
                game.setCounters("Unforgiving One", 1)
                val oneDropCard = game.findCardsInGraveyard(1, "Test One-Drop").single()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Unforgiving One" to 2)).error shouldBe null
                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(oneDropCard)).error shouldBe null

                game.setCounters("Unforgiving One", 0)
                game.resolveStack()

                withClue("X fell to 0 before resolution, so the target became illegal") {
                    game.isInGraveyard(1, "Test One-Drop") shouldBe true
                    game.findPermanent("Test One-Drop") shouldBe null
                }
            }
        }
    }
}
