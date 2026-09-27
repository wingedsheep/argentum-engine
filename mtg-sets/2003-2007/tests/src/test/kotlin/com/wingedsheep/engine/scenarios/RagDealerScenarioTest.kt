package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.RagDealer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rag Dealer — "{2}{B}, {T}: Exile up to three target cards from a single graveyard."
 *
 * Proves the three-card cap, the single-graveyard constraint, and that fewer targets is legal.
 */
class RagDealerScenarioTest : ScenarioTestBase() {

    private val exileAbility = RagDealer.activatedAbilities.single().id

    init {
        fun graveyardTargets(game: TestGame, ownerNumber: Int, names: List<String>): List<ChosenTarget> {
            val ownerId = if (ownerNumber == 1) game.player1Id else game.player2Id
            return names.map { name ->
                ChosenTarget.Card(game.findCardsInGraveyard(ownerNumber, name).first(), ownerId, Zone.GRAVEYARD)
            }
        }

        fun board() = scenario().withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Rag Dealer")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Hill Giant")
            .withCardInGraveyard(2, "Island")
            .withCardInGraveyard(2, "Forest")
            .withCardInGraveyard(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun activate(game: TestGame, targets: List<ChosenTarget>) = game.execute(
            ActivateAbility(game.player1Id, game.findPermanent("Rag Dealer")!!, exileAbility, targets = targets)
        )

        test("exiles three cards from one graveyard") {
            val game = board().build()
            activate(game, graveyardTargets(game, 2, listOf("Hill Giant", "Island", "Forest"))).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInExile(2, "Island") shouldBe true
            game.isInExile(2, "Forest") shouldBe true
            game.isInGraveyard(2, "Mountain") shouldBe true
        }

        test("cannot choose more than three targets") {
            val game = board().build()
            activate(
                game,
                graveyardTargets(game, 2, listOf("Hill Giant", "Island", "Forest", "Mountain"))
            ).error shouldNotBe null
            game.graveyardSize(2) shouldBe 4
        }

        test("targets may not be spread across two graveyards") {
            val game = board().build()
            activate(
                game,
                graveyardTargets(game, 1, listOf("Grizzly Bears")) + graveyardTargets(game, 2, listOf("Hill Giant"))
            ).error shouldNotBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("fewer targets is legal, including its controller's own graveyard") {
            val game = board().build()
            activate(game, graveyardTargets(game, 1, listOf("Grizzly Bears"))).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.graveyardSize(2) shouldBe 4
        }
    }
}
