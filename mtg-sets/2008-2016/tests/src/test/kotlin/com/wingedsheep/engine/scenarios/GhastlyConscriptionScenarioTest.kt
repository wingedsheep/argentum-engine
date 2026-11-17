package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.GhastlyConscription
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Ghastly Conscription (FRF #70) — {5}{B}{B} Sorcery.
 * "Exile all creature cards from target player's graveyard in a face-down pile, shuffle that pile, then
 * manifest those cards."
 *
 * Covers the mixed-ownership case the snapshot can't see: an opponent's creature cards are manifested
 * under the caster's control (they stay owned by the opponent), and non-creature cards stay behind.
 */
class GhastlyConscriptionScenarioTest : FunSpec({

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + GhastlyConscription)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(d, you, d.getOpponent(you))
    }

    fun GameTestDriver.faceDownCreatures(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<FaceDownComponent>() == true }

    fun GameTestDriver.cast(you: EntityId, target: EntityId) {
        val spell = putCardInHand(you, "Ghastly Conscription")
        giveMana(you, Color.BLACK, 7)
        castSpell(you, spell, listOf(target)).error shouldBe null
        bothPass()
    }

    test("manifests the opponent's creature cards under your control and leaves the rest") {
        val (d, you, opponent) = setUp()
        val courser = d.putCardInGraveyard(opponent, "Centaur Courser")
        val lions = d.putCardInGraveyard(opponent, "Savannah Lions")
        val bolt = d.putCardInGraveyard(opponent, "Lightning Bolt")

        d.cast(you, opponent)

        val manifested = d.faceDownCreatures(you)
        manifested.size shouldBe 2
        manifested shouldNotContain courser
        manifested shouldNotContain lions
        manifested.map { d.state.getEntity(it)!!.get<CardComponent>()!!.name } shouldContainExactlyInAnyOrder
            listOf("Centaur Courser", "Savannah Lions")
        manifested.forEach { id ->
            d.state.getEntity(id)!!.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
            d.state.getEntity(id)!!.get<CardComponent>()!!.ownerId shouldBe opponent
            d.state.projectedState.getController(id) shouldBe you
            d.state.projectedState.getPower(id) shouldBe 2
            d.state.projectedState.getToughness(id) shouldBe 2
        }
        d.faceDownCreatures(opponent) shouldBe emptyList()
        d.getGraveyard(opponent) shouldBe listOf(bolt)
        d.getExile(opponent) shouldBe emptyList()
        d.getExile(you) shouldBe emptyList()
    }

    test("an empty creature pool resolves without manifesting anything") {
        val (d, you, opponent) = setUp()
        val bolt = d.putCardInGraveyard(opponent, "Lightning Bolt")

        d.cast(you, opponent)

        d.faceDownCreatures(you) shouldBe emptyList()
        d.getGraveyard(opponent) shouldBe listOf(bolt)
    }
})
