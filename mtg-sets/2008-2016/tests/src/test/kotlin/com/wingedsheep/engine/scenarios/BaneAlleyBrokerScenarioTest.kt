package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bane Alley Broker (GTC #145) — {1}{U}{B} Creature — Human Rogue 0/3.
 *
 *   {T}: Draw a card, then exile a card from your hand face down.
 *   You may look at cards exiled with this creature.
 *   {U}{B}, {T}: Return a card exiled with this creature to its owner's hand.
 *
 * Pins the look as a static of the permanent: its controller sees the pile and the opponent
 * doesn't; a player who gains control starts seeing it, and keeps seeing it after control
 * reverts (2013-01-24 ruling); the return ability sends a card to its owner's hand.
 */
class BaneAlleyBrokerScenarioTest : ScenarioTestBase() {

    private val drawExile get() = cardRegistry.getCard("Bane Alley Broker")!!.activatedAbilities[0].id
    private val returnCard get() = cardRegistry.getCard("Bane Alley Broker")!!.activatedAbilities[1].id

    /** What [viewer] knows of each exiled card: its own name, or the identity revealed under a face-down card. */
    private fun TestGame.exileNames(viewer: Int): List<String> =
        getClientState(viewer).cards.values.filter { it.zone?.zoneType == Zone.EXILE }.map { it.revealedName ?: it.name }

    private fun TestGame.exiled(player: Int, name: String): EntityId =
        state.getExile(if (player == 1) player1Id else player2Id)
            .single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("the controller sees the face-down pile, the opponent doesn't, and the return puts it back in hand") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Bane Alley Broker")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val broker = game.findPermanent("Bane Alley Broker")!!

            game.execute(ActivateAbility(game.player1Id, broker, drawExile)).error shouldBe null
            game.resolveStack()
            withClue("the draw happened before the exile choice") { game.isInHand(1, "Grizzly Bears") shouldBe true }
            val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
            game.selectCards(listOf(bolt))

            val exiledBolt = game.exiled(1, "Lightning Bolt")
            game.state.getEntity(exiledBolt)!!.has<FaceDownComponent>() shouldBe true
            withClue("Alice controls the Broker, so she may look") { game.exileNames(1) shouldBe listOf("Lightning Bolt") }
            withClue("Bob may not") { game.exileNames(2).contains("Lightning Bolt") shouldBe false }

            game.state = game.state.updateEntity(broker) { it.without<TappedComponent>() }
            game.execute(ActivateAbility(game.player1Id, broker, returnCard)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(listOf(exiledBolt))

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.state.getExile(game.player1Id).isEmpty() shouldBe true
        }

        test("a player who gains control may look, and keeps looking after control reverts") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(2, "Bane Alley Broker")
                .withCardInExile(2, "Hill Giant")
                .withCardInHand(1, "Act of Treason")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val broker = game.findPermanent("Bane Alley Broker")!!
            val giant = game.exiled(2, "Hill Giant")
            // Bob exiled the Giant with this Broker on an earlier turn.
            game.state = game.state
                .updateEntity(giant) { it.with(FaceDownComponent) }
                .updateEntity(broker) { it.with(LinkedExileComponent(listOf(giant))) }

            withClue("before the steal: Bob looks, Alice doesn't") {
                game.exileNames(2) shouldBe listOf("Hill Giant")
                game.exileNames(1).contains("Hill Giant") shouldBe false
            }

            game.castSpell(1, "Act of Treason", broker).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getController(broker) shouldBe game.player1Id
            withClue("Alice controls the Broker now, so she may look") { game.exileNames(1) shouldBe listOf("Hill Giant") }
            withClue("Bob keeps the look he already had") { game.exileNames(2) shouldBe listOf("Hill Giant") }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getController(broker) shouldBe game.player2Id
            withClue("control reverted, but Alice keeps looking for as long as it stays exiled") {
                game.exileNames(1) shouldBe listOf("Hill Giant")
            }
        }

        test("the return ability sends a card to its owner's hand, not the activating player's") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Bane Alley Broker")
                .withCardInExile(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val broker = game.findPermanent("Bane Alley Broker")!!
            val giant = game.exiled(2, "Hill Giant")
            // Bob's Giant was exiled with the Broker while Bob controlled it.
            game.state = game.state
                .updateEntity(giant) { it.with(FaceDownComponent) }
                .updateEntity(broker) { it.with(LinkedExileComponent(listOf(giant))) }

            game.execute(ActivateAbility(game.player1Id, broker, returnCard)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(listOf(giant))

            game.isInHand(2, "Hill Giant") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe false
        }
    }
}
