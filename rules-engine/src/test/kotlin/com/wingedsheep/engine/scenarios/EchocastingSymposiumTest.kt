package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.ParadigmComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.EchocastingSymposium
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Echocasting Symposium (SOS) — {4}{U}{U} Sorcery — Lesson.
 *
 * "Target player creates a token that's a copy of target creature you control. Paradigm (...)"
 *
 * Pins: the token copy of the caster's creature is created under the *target player's* control
 * (here, the opponent), proving the `controller` override on the copy effect; and the resolved
 * spell self-exiles with the Paradigm marker.
 */
class EchocastingSymposiumTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + EchocastingSymposium)
        return driver
    }

    fun tokenCopiesOf(driver: GameTestDriver, playerId: EntityId, name: String): List<EntityId> =
        driver.state.getZone(playerId, Zone.BATTLEFIELD).filter { id ->
            val e = driver.state.getEntity(id)
            e?.has<TokenComponent>() == true && e.get<CardComponent>()?.name == name
        }

    test("target player (the opponent) gets a token copy of your creature; spell self-exiles via Paradigm") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myCreature = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(me, "Echocasting Symposium")
        driver.giveColorlessMana(me, 4)
        driver.giveMana(me, Color.BLUE, 2)

        // Targets: target player = opponent, target creature you control = my Centaur Courser.
        driver.castSpellWithTargets(
            me, spell,
            listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(myCreature)),
        ).error shouldBe null
        driver.bothPass()

        // The opponent (target player) controls a token copy of Centaur Courser.
        tokenCopiesOf(driver, opponent, "Centaur Courser").size shouldBe 1
        // I do not get a token.
        tokenCopiesOf(driver, me, "Centaur Courser").size shouldBe 0

        // Paradigm: the resolved spell self-exiles with the marker.
        val exiled = driver.state.getZone(me, Zone.EXILE)
            .mapNotNull { driver.state.getEntity(it) }
            .filter { it.get<CardComponent>()?.name == "Echocasting Symposium" }
        exiled.any { it.get<ParadigmComponent>() != null } shouldBe true
    }
})
