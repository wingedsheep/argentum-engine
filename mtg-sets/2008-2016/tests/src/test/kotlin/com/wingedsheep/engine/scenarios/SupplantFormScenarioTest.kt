package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Supplant Form (FRF #54) — "Return target creature to its owner's hand. You create a token that's
 * a copy of that creature."
 *
 * The creature has left the battlefield by the time the token is made, so the copy reads its
 * last-known copiable values (CR 608.2h, CR 707.2) — not the card in hand, and not its pumps.
 */
class SupplantFormScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.tokens(): List<EntityId> =
            state.getBattlefield().filter { state.getEntity(it)?.has<TokenComponent>() == true }

        fun TestGame.controllerOf(id: EntityId): EntityId? =
            state.getEntity(id)?.get<ControllerComponent>()?.playerId

        test("an opponent's creature goes to their hand and you get an unpumped copy of it") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Supplant Form")
                .withLandsOnBattlefield(1, "Island", 6)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Giant Growth", wurm).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Supplant Form", wurm).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Craw Wurm") shouldBe true
            val tokens = game.tokens()
            tokens shouldHaveSize 1
            val token = tokens.single()
            game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Craw Wurm"
            game.controllerOf(token) shouldBe game.player1Id
            withClue("Giant Growth's +3/+3 is not a copiable value: the token is a plain 6/4") {
                game.state.projectedState.getPower(token) shouldBe 6
                game.state.projectedState.getToughness(token) shouldBe 4
            }
        }

        test("a Clone is copied as the creature it was copying") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInHand(2, "Clone")
                .withCardInHand(1, "Supplant Form")
                .withLandsOnBattlefield(1, "Island", 6)
                .withLandsOnBattlefield(2, "Island", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(2, "Clone").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(wurm)).error shouldBe null
            val clone = game.findPermanents("Craw Wurm").single { it != wurm }
            game.passPriority() // the active player passes; Supplant Form is cast in response

            game.castSpell(1, "Supplant Form", clone).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Clone") shouldBe true
            val token = game.tokens().single()
            game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Craw Wurm"
            game.controllerOf(token) shouldBe game.player1Id
            game.state.projectedState.getPower(token) shouldBe 6
            game.state.projectedState.getToughness(token) shouldBe 4
        }

        test("a face-down creature yields a face-up nameless 2/2 with no abilities") {
            val game = scenario().withPlayers()
                .withCardInHand(2, "Aerie Bowmasters")
                .withCardInHand(1, "Supplant Form")
                .withLandsOnBattlefield(1, "Island", 6)
                .withLandsOnBattlefield(2, "Forest", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bowmasters = game.findCardsInHand(2, "Aerie Bowmasters").single()
            game.execute(CastSpell(game.player2Id, bowmasters, emptyList(), castFaceDown = true)).error shouldBe null
            game.resolveStack()
            val faceDown = game.state.getBattlefield()
                .single { game.state.getEntity(it)?.has<FaceDownComponent>() == true }
            game.passPriority()

            game.castSpell(1, "Supplant Form", faceDown).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Aerie Bowmasters") shouldBe true
            val token = game.tokens().single()
            val entity = game.state.getEntity(token)!!
            withClue("the token is face up, not a hidden Aerie Bowmasters") {
                entity.has<FaceDownComponent>() shouldBe false
                entity.get<CardComponent>()?.name shouldBe ""
            }
            game.controllerOf(token) shouldBe game.player1Id
            game.state.projectedState.getPower(token) shouldBe 2
            game.state.projectedState.getToughness(token) shouldBe 2
            game.state.projectedState.getKeywords(token).shouldBeEmpty()
            game.state.projectedState.getSubtypes(token).shouldBeEmpty()
        }

        test("if the target is gone on resolution, no token is created") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Supplant Form")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 6)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Supplant Form", bears).error shouldBe null
            game.passPriority() // the opponent kills the target in response
            game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.tokens().shouldBeEmpty()
        }
    }
}
