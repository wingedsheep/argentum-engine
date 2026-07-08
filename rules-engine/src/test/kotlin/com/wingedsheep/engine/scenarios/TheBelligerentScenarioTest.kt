package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.TheBelligerent
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for The Belligerent.
 *
 * The Belligerent: {2}{U}{R}
 * Legendary Artifact — Vehicle
 * 5/5
 * Whenever The Belligerent attacks, create a Treasure token. Until end of turn, you may look at the
 * top card of your library any time, and you may play lands and cast spells from the top of your
 * library.
 * Crew 3
 *
 * The attack trigger creates the Treasure token; the "until end of turn" play window is modeled as
 * two conditional statics gated on "attacked this turn" (the Lunar Whale pattern). These tests
 * exercise both halves: the Treasure fires on attack, and the play-from-top permission is dormant
 * until the Vehicle is crewed and attacks, then active for the rest of the turn.
 */
class TheBelligerentScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TheBelligerent)
        // The attack trigger's CreateTreasure looks up the "Treasure" token definition in the
        // registry, so it must be registered alongside the card.
        driver.registerCard(PredefinedTokens.Treasure)
        return driver
    }

    test("cannot play the top card of library before The Belligerent attacks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        // Belligerent present but it has not attacked this turn — the conditional gate is false.
        driver.putPermanentOnBattlefield(you, "The Belligerent")
        val mountainOnTop = driver.putCardOnTopOfLibrary(you, "Mountain")

        // A land on top of library can only be played via the (currently inactive) permission.
        driver.playLand(you, mountainOnTop).isSuccess shouldBe false
        driver.findPermanent(you, "Mountain") shouldBe null
    }

    test("attacking creates a Treasure token and opens the play-from-top window") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }

        // Crew partner: Centaur Courser (power 3 satisfies Crew 3).
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val belligerent = driver.putPermanentOnBattlefield(you, "The Belligerent")
        driver.removeSummoningSickness(belligerent)

        // Crew the Belligerent (it becomes a creature) and declare it as an attacker.
        driver.submitSuccess(CrewVehicle(you, belligerent, listOf(courser)))
        driver.bothPass() // resolve the crew activation
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitSuccess(DeclareAttackers(you, mapOf(belligerent to opponent)))
        driver.bothPass() // resolve the attack trigger (creates the Treasure token)

        // The attack trigger created a Treasure token.
        driver.findPermanent(you, "Treasure") shouldNotBe null

        // Move to the postcombat main phase — the Belligerent "attacked this turn", gate is open.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        val mountainOnTop = driver.putCardOnTopOfLibrary(you, "Mountain")
        driver.playLand(you, mountainOnTop).isSuccess shouldBe true
        driver.findPermanent(you, "Mountain") shouldNotBe null
    }

    test("can cast a spell from the top of library after The Belligerent attacks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val belligerent = driver.putPermanentOnBattlefield(you, "The Belligerent")
        driver.removeSummoningSickness(belligerent)

        driver.submitSuccess(CrewVehicle(you, belligerent, listOf(courser)))
        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitSuccess(DeclareAttackers(you, mapOf(belligerent to opponent)))
        driver.bothPass()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Frogmite (an artifact creature) on top — any spell type may be cast from the top.
        val frogmiteOnTop = driver.putCardOnTopOfLibrary(you, "Frogmite")
        driver.giveMana(you, Color.BLUE, 4)

        driver.castSpell(you, frogmiteOnTop).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getBattlefield(you).contains(frogmiteOnTop) shouldBe true
    }
})
