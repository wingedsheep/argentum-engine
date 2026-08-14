package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Riverchurn Monument (DFT #57).
 *
 * Riverchurn Monument {1}{U} — Artifact
 * {1}, {T}: Any number of target players each mill two cards.
 * Exhaust — {2}{U}{U}, {T}: Any number of target players each mill cards equal to the number of
 * cards in their graveyard.
 *
 * The load-bearing claims:
 *  - "any number of target players" accepts more than one player, and each chosen player mills
 *    from *their own* library into *their own* graveyard;
 *  - a single target is legal too ("any number" includes one), and untargeted players mill nothing;
 *  - the exhaust amount is each targeted player's own graveyard size, evaluated per player — not
 *    the controller's, and not one shared number applied to everyone.
 *
 * The once-per-object half of Exhaust is covered generically by [ExhaustKeywordScenarioTest].
 */
class RiverchurnMonumentScenarioTest : ScenarioTestBase() {

    private val monument get() = cardRegistry.getCard("Riverchurn Monument")!!
    private val basicAbilityId get() = monument.script.activatedAbilities[0].id
    private val exhaustAbilityId get() = monument.script.activatedAbilities[1].id

    init {
        context("Riverchurn Monument") {

            test("{1}, {T}: both targeted players each mill two cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Riverchurn Monument")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .also { b -> repeat(10) { b.withCardInLibrary(1, "Grizzly Bears") } }
                    .also { b -> repeat(10) { b.withCardInLibrary(2, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monumentId = game.findPermanent("Riverchurn Monument")!!
                val p1LibraryBefore = game.librarySize(1)
                val p2LibraryBefore = game.librarySize(2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monumentId,
                        abilityId = basicAbilityId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Player(game.player2Id)
                        )
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("each targeted player mills from their own library") {
                    game.librarySize(1) shouldBe p1LibraryBefore - 2
                    game.librarySize(2) shouldBe p2LibraryBefore - 2
                }
                game.graveyardSize(1) shouldBe 2
                game.graveyardSize(2) shouldBe 2
            }

            test("{1}, {T}: one target is legal and only that player mills") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Riverchurn Monument")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .also { b -> repeat(10) { b.withCardInLibrary(1, "Grizzly Bears") } }
                    .also { b -> repeat(10) { b.withCardInLibrary(2, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monumentId = game.findPermanent("Riverchurn Monument")!!
                val p1LibraryBefore = game.librarySize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monumentId,
                        abilityId = basicAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.graveyardSize(2) shouldBe 2
                withClue("the untargeted controller mills nothing") {
                    game.librarySize(1) shouldBe p1LibraryBefore
                    game.graveyardSize(1) shouldBe 0
                }
            }

            test("exhaust mills each player their own graveyard's worth, not a shared count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Riverchurn Monument")
                    .withLandsOnBattlefield(1, "Island", 4)
                    // Asymmetric graveyards: p1 has 1 card, p2 has 3.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .also { b -> repeat(10) { b.withCardInLibrary(1, "Grizzly Bears") } }
                    .also { b -> repeat(10) { b.withCardInLibrary(2, "Grizzly Bears") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monumentId = game.findPermanent("Riverchurn Monument")!!
                val p1LibraryBefore = game.librarySize(1)
                val p2LibraryBefore = game.librarySize(2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monumentId,
                        abilityId = exhaustAbilityId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Player(game.player2Id)
                        )
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("player 1 had 1 card in their graveyard, so mills 1") {
                    game.librarySize(1) shouldBe p1LibraryBefore - 1
                }
                withClue("player 2 had 3 cards in their graveyard, so mills 3") {
                    game.librarySize(2) shouldBe p2LibraryBefore - 3
                }
            }
        }
    }
}
