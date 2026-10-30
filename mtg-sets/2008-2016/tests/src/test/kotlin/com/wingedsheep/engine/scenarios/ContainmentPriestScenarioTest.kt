package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.c14.cards.ContainmentPriest
import com.wingedsheep.mtg.sets.definitions.fut.cards.DryadArbor
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Containment Priest (C14 #5) — {1}{W} Creature — Human Cleric 2/2.
 *
 *   "Flash
 *    If a nontoken creature would enter and it wasn't cast, exile it instead."
 *
 * A battlefield-entry `RedirectZoneChange` filtered to `Creature.nontoken().notCast()`: reanimated,
 * flickered, manifested and land-played creatures go to exile; cast creatures and tokens enter.
 */
class ContainmentPriestScenarioTest : FunSpec({

    val manifestTop = card("Manifest Top") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.manifest() }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                ContainmentPriest, Zombify, Cloudshift, DryadArbor, GrizzlyBears, RaiseTheAlarm, manifestTop
            )
        )
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.exile(player: com.wingedsheep.sdk.model.EntityId) = state.getZone(player, Zone.EXILE)

    test("a creature reanimated under an opponent's Priest is exiled instead") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Containment Priest")
        val bears = d.putCardInGraveyard(me, "Grizzly Bears")

        val spell = d.putCardInHand(me, "Zombify")
        d.giveMana(me, Color.BLACK, 4)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(bears, me, Zone.GRAVEYARD))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldNotContain bears
        d.exile(me) shouldContain bears
    }

    test("a cast creature spell resolves onto the battlefield") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")

        val bears = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, bears).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldContain bears
    }

    test("tokens still enter") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val before = d.getPermanents(me).size

        val spell = d.putCardInHand(me, "Raise the Alarm")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpell(me, spell).error shouldBe null
        d.resolveStack()

        d.getPermanents(me).size shouldBe before + 2
    }

    test("a flickered creature doesn't come back — it's exiled as it would return") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val bears = d.putPermanentOnBattlefield(me, "Grizzly Bears")

        val spell = d.putCardInHand(me, "Cloudshift")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Permanent(bears))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me).none { d.getCardName(it) == "Grizzly Bears" } shouldBe true
        d.exile(me).any { d.getCardName(it) == "Grizzly Bears" } shouldBe true
    }

    test("a manifested card is a creature entering, so it is exiled") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val top = d.putCardOnTopOfLibrary(me, "Plains")

        val spell = d.putCardInHand(me, "Manifest Top")
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, spell).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldNotContain top
        d.exile(me) shouldContain top
    }

    test("Dryad Arbor played as a land is exiled, and the land drop is spent") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Containment Priest")
        val arbor = d.putCardInHand(me, "Dryad Arbor")
        val plains = d.putCardInHand(me, "Plains")

        d.playLand(me, arbor).error shouldBe null

        d.getPermanents(me) shouldNotContain arbor
        d.exile(me) shouldContain arbor
        d.playLand(me, plains).error shouldBe "You have already played a land this turn"
    }

    test("the Priest entering without being cast doesn't exile itself") {
        val d = driver()
        val me = d.activePlayer!!
        val priest = d.putCardInGraveyard(me, "Containment Priest")

        val spell = d.putCardInHand(me, "Zombify")
        d.giveMana(me, Color.BLACK, 4)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(priest, me, Zone.GRAVEYARD))).error shouldBe null
        d.resolveStack()

        d.getPermanents(me) shouldContain priest
    }
})
