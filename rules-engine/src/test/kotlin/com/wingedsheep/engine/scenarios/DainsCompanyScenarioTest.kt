package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dáin's Company (HOB #152) — {R}{W} Creature — Dwarf Warrior 2/2
 *
 * This creature has lifelink as long as you control another Dwarf.
 * When this creature enters, look at the top four cards of your library. You may reveal a Dwarf or
 * Equipment card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 *
 * Two claims worth proving: the lifelink clause is live/dark on *another* Dwarf rather than latching
 * on once, and the dig's "Dwarf or Equipment" filter really admits both halves (a single
 * `HasAnyOfSubtypes`, not one subtype with the other silently dropped).
 */
class DainsCompanyScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Dáin's Company") {

            test("lifelink is dark alone and live once another Dwarf joins") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dáin's Company")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dain = game.findPermanent("Dáin's Company")!!
                withClue("Alone, it is not another Dwarf to itself") {
                    projector.project(game.state).hasKeyword(dain, Keyword.LIFELINK) shouldBe false
                }

                val withFriend = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dáin's Company")
                    .withCardOnBattlefield(1, "Dwarven Mauler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dain2 = withFriend.findPermanent("Dáin's Company")!!
                withClue("A second Dwarf turns the clause on") {
                    projector.project(withFriend.state).hasKeyword(dain2, Keyword.LIFELINK) shouldBe true
                }
            }

            test("the dig offers both Dwarf and Equipment cards, and skips the rest") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dáin's Company")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    // Top four, in library order: an Equipment, a Dwarf, and two of neither.
                    .withCardInLibrary(1, "Ordinary Bear")
                    .withCardInLibrary(1, "Large Bear")
                    .withCardInLibrary(1, "Dwarven Mauler")
                    .withCardInLibrary(1, "Dwarven Shortsword")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dáin's Company").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("The ETB dig should pause to pick a card") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                val selectable = (decision as SelectCardsDecision).options.map { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name
                }.toSet()

                withClue("Both the Dwarf and the Equipment are legal picks: $selectable") {
                    selectable.contains("Dwarven Mauler") shouldBe true
                    selectable.contains("Dwarven Shortsword") shouldBe true
                }
                withClue("The two Bears are neither Dwarf nor Equipment: $selectable") {
                    selectable.contains("Ordinary Bear") shouldBe false
                    selectable.contains("Large Bear") shouldBe false
                }
            }

            test("declining the reveal is legal even with a hit present") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dáin's Company")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Ordinary Bear")
                    .withCardInLibrary(1, "Large Bear")
                    .withCardInLibrary(1, "Little Bear")
                    .withCardInLibrary(1, "Dwarven Mauler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dáin's Company").error shouldBe null
                game.resolveStack()
                game.skipSelection()
                game.resolveStack()

                withClue("Declining leaves the hand empty — 'you may reveal'") {
                    game.isInHand(1, "Dwarven Mauler") shouldBe false
                }
                withClue("All four looked-at cards went to the bottom of the library") {
                    game.librarySize(1) shouldBe 4
                }
            }
        }
    }
}
