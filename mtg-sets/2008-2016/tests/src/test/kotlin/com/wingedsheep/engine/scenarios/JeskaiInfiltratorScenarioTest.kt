package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.JeskaiInfiltrator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jeskai Infiltrator (FRF #36) — {2}{U} 2/3 Human Monk.
 * "This creature can't be blocked as long as you control no other creatures.
 * When this creature deals combat damage to a player, exile it and the top card of your library in a
 * face-down pile, shuffle that pile, then manifest those cards."
 */
class JeskaiInfiltratorScenarioTest : FunSpec({

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + JeskaiInfiltrator)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(d, you, d.getOpponent(you))
    }

    fun GameTestDriver.faceDownCreatures(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<FaceDownComponent>() == true }

    /** Attack with [attacker] unblocked and stop once its combat-damage trigger is on the stack. */
    fun GameTestDriver.connectWith(you: EntityId, opponent: EntityId, attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(you, listOf(attacker), opponent).error shouldBe null
        bothPass()
        declareNoBlockers(opponent).error shouldBe null
        repeat(10) { if (state.stack.isEmpty() && !isPaused) bothPass() }
        state.stack.size shouldBe 1
    }

    test("can't be blocked while you control no other creatures, but can be once you do") {
        val (d, you, opponent) = setUp()
        val infiltrator = d.putCreatureOnBattlefield(you, "Jeskai Infiltrator")
        d.removeSummoningSickness(infiltrator)
        val blocker = d.putCreatureOnBattlefield(opponent, "Centaur Courser")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(infiltrator), opponent).error shouldBe null
        d.bothPass()
        d.declareBlockers(opponent, mapOf(blocker to listOf(infiltrator))).error shouldNotBe null

        val (d2, you2, opponent2) = setUp()
        val infiltrator2 = d2.putCreatureOnBattlefield(you2, "Jeskai Infiltrator")
        d2.removeSummoningSickness(infiltrator2)
        d2.putCreatureOnBattlefield(you2, "Centaur Courser")
        val blocker2 = d2.putCreatureOnBattlefield(opponent2, "Centaur Courser")
        d2.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d2.declareAttackers(you2, listOf(infiltrator2), opponent2).error shouldBe null
        d2.bothPass()
        d2.declareBlockers(opponent2, mapOf(blocker2 to listOf(infiltrator2))).error shouldBe null
    }

    test("connecting exiles it and the top card face down, then manifests both under fresh identities") {
        val (d, you, opponent) = setUp()
        val infiltrator = d.putCreatureOnBattlefield(you, "Jeskai Infiltrator")
        d.removeSummoningSickness(infiltrator)
        val top = d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.connectWith(you, opponent, infiltrator)
        d.bothPass()

        d.assertLifeTotal(opponent, 18)
        val manifested = d.faceDownCreatures(you)
        manifested.size shouldBe 2
        // The shuffle disguises which is which: neither keeps the id an opponent saw going in.
        manifested shouldNotContain infiltrator
        manifested shouldNotContain top
        d.state.getEntity(infiltrator) shouldBe null
        d.state.getEntity(top) shouldBe null
        manifested.map { d.state.getEntity(it)!!.get<FaceDownModeComponent>()?.mode } shouldBe
            listOf(FaceDownMode.MANIFEST, FaceDownMode.MANIFEST)
        manifested.map { d.state.getEntity(it)!!.get<CardComponent>()!!.name } shouldContainExactlyInAnyOrder
            listOf("Jeskai Infiltrator", "Centaur Courser")
        d.state.getExile(you) shouldBe emptyList()
        manifested.forEach { d.state.projectedState.getPower(it) shouldBe 2 }

        // Manifested creature cards turn face up for their mana cost — the Infiltrator for {2}{U}.
        val faceDownInfiltrator = manifested.single {
            d.state.getEntity(it)!!.get<CardComponent>()!!.name == "Jeskai Infiltrator"
        }
        d.giveMana(you, Color.BLUE, 3)
        d.submit(TurnFaceUp(playerId = you, sourceId = faceDownInfiltrator)).error shouldBe null
        d.state.getEntity(faceDownInfiltrator)!!.has<FaceDownComponent>() shouldBe false
        d.state.projectedState.getToughness(faceDownInfiltrator) shouldBe 3
    }

    test("if it has left the battlefield by resolution, only the top card is manifested") {
        val (d, you, opponent) = setUp()
        val infiltrator = d.putCreatureOnBattlefield(you, "Jeskai Infiltrator")
        d.removeSummoningSickness(infiltrator)
        d.putCardOnTopOfLibrary(you, "Centaur Courser")

        d.connectWith(you, opponent, infiltrator)
        d.moveToGraveyard(infiltrator)
        d.bothPass()

        val manifested = d.faceDownCreatures(you).singleOrNull().shouldNotBeNull()
        d.state.getEntity(manifested)!!.get<CardComponent>()!!.name shouldBe "Centaur Courser"
        d.state.getZone(you, Zone.GRAVEYARD) shouldContainExactlyInAnyOrder listOf(infiltrator)
    }
})
