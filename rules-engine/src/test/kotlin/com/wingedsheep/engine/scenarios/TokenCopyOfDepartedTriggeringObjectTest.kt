package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Whenever a creature … enters, create a token that's a copy of that creature" when the creature
 * has left the battlefield before the ability resolves — exercised through Necroduality ("Whenever
 * a nontoken Zombie you control enters, create a token that's a copy of that creature").
 *
 * CR 608.2h: an effect that needs information from an object that has left the zone it was
 * expected in uses that object's last known information. The copy is therefore made from the
 * copiable values (CR 707.2) the creature had as it last existed on the battlefield, frozen into
 * its departure snapshot — the Molten Echoes / Necroduality rulings ("the token that's created
 * uses the creature's last known information from before it left").
 */
class TokenCopyOfDepartedTriggeringObjectTest : ScenarioTestBase() {

    private val zombie = card("Test Shambling Zombie") {
        manaCost = "{1}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
    }

    private fun TestGame.tokensNamed(name: String): List<EntityId> =
        findAllPermanents(name).filter { id ->
            state.getEntity(id)?.has<TokenComponent>() == true
        }

    init {
        cardRegistry.register(zombie)

        test("the departure snapshot carries the creature's copiable values") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Shambling Zombie")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val zombieId = game.findPermanent("Test Shambling Zombie")!!

            game.castSpell(1, "Lightning Bolt", zombieId).error shouldBe null
            game.resolveStack()

            val snapshot = game.state.getEntity(zombieId)!!.get<LastKnownPermanentComponent>()!!.snapshot
            snapshot.copiableCard shouldNotBe null
            snapshot.copiableCard!!.name shouldBe "Test Shambling Zombie"
        }

        test("a creature killed in response is copied from last-known information") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Necroduality")
                .withCardInHand(1, "Test Shambling Zombie")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Shambling Zombie").error shouldBe null
            game.passPriority()
            game.passPriority()
            // The Zombie resolved; Necroduality's trigger is waiting on the stack.
            game.state.stack shouldHaveSize 1
            val zombieId = game.findPermanent("Test Shambling Zombie")!!

            game.castSpell(1, "Lightning Bolt", zombieId).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Shambling Zombie") shouldBe true
            val tokens = game.tokensNamed("Test Shambling Zombie")
            tokens shouldHaveSize 1
            game.state.getEntity(tokens.single())!!.get<CardComponent>()!!.typeLine.subtypes
                .map { it.value } shouldBe listOf("Zombie")
            game.state.projectedState.getPower(tokens.single()) shouldBe 2
        }

        test("with the creature still on the battlefield the copy is made as usual") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Necroduality")
                .withCardInHand(1, "Test Shambling Zombie")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Shambling Zombie").error shouldBe null
            game.resolveStack()

            game.tokensNamed("Test Shambling Zombie") shouldHaveSize 1
            game.findAllPermanents("Test Shambling Zombie") shouldHaveSize 2
        }
    }
}
