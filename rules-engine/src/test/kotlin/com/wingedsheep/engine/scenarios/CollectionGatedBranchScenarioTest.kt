package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pins the backlog §2.1 guarantee that collection-gated branches work end to end:
 * a `Gate.WhenCondition(CollectionContainsMatch(...))` (here via `ConditionalEffect`)
 * evaluated AFTER a pipeline pause still sees the collections stored by earlier steps.
 *
 * Cache Grab (BLB) is the in-corpus shape: Mill 4 → SelectFromCollection (pauses for a
 * player decision) → MoveCollection → conditional on `CollectionContainsMatch("selected",
 * Squirrel)`. The condition reads a collection that only exists because the paused
 * selection's result was merged back into the resumed pipeline context.
 */
class CollectionGatedBranchScenarioTest : ScenarioTestBase() {

    private val testSquirrel = card("Test Squirrel") {
        manaCost = "{G}"
        typeLine = "Creature — Squirrel"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(testSquirrel)

        fun graveyardCardId(game: TestGame, name: String): EntityId? =
            game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD))
                .firstOrNull { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

        context("collection-gated branch evaluated after a pipeline pause") {

            test("selecting a Squirrel from the milled cards satisfies the gate (Food token created)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cache Grab")
                    .withCardInLibrary(1, "Test Squirrel")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cache Grab").error shouldBe null
                game.resolveStack()

                withClue("the select-from-milled step should pause for a decision") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                val squirrelId = graveyardCardId(game, "Test Squirrel")
                withClue("milled Squirrel should be in the graveyard at decision time") {
                    squirrelId shouldNotBe null
                }
                game.selectCards(listOf(squirrelId!!))
                game.resolveStack()

                withClue("the Squirrel should be returned to hand") {
                    game.isInHand(1, "Test Squirrel") shouldBe true
                }
                withClue("gate sees the post-pause 'selected' collection ⇒ Food token created") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }

            test("selecting a non-Squirrel permanent fails the gate (no Food token)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cache Grab")
                    .withCardInLibrary(1, "Test Squirrel")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cache Grab").error shouldBe null
                game.resolveStack()

                (game.state.pendingDecision != null) shouldBe true
                val bearsId = graveyardCardId(game, "Grizzly Bears")
                bearsId shouldNotBe null
                game.selectCards(listOf(bearsId!!))
                game.resolveStack()

                withClue("a non-Squirrel selection must not create a Food token") {
                    game.findPermanents("Food").size shouldBe 0
                }
            }

            test("declining the selection leaves the gated collection empty (no Food token)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cache Grab")
                    .withCardInLibrary(1, "Test Squirrel")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cache Grab").error shouldBe null
                game.resolveStack()

                (game.state.pendingDecision != null) shouldBe true
                game.skipSelection()
                game.resolveStack()

                withClue("an empty 'selected' collection must fail CollectionContainsMatch") {
                    game.findPermanents("Food").size shouldBe 0
                }
                withClue("the Squirrel stays in the graveyard") {
                    game.isInGraveyard(1, "Test Squirrel") shouldBe true
                }
            }
        }
    }
}
