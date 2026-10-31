package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.GrafdiggersCage
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ThinkTwice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Grafdigger's Cage (DKA #149) — {1} Artifact.
 *
 * "Creature cards in graveyards and libraries can't enter the battlefield.
 *  Players can't cast spells from graveyards or libraries."
 *
 * The card that makes the entry lock judge a face-down entry as the 2/2 creature it would be:
 * manifesting even a land from a library is impossible while the Cage is out (ruling 2019-07-12).
 */
class GrafdiggersCageScenarioTest : FunSpec({

    val manifestTop = card("Manifest Top") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.manifest() }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(GrafdiggersCage, Zombify, ThinkTwice, manifestTop))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.castableCards(player: EntityId): Set<EntityId> =
        legalActions(player).filter { it.affordable }.mapNotNull { (it.action as? CastSpell)?.cardId }.toSet()

    test("a creature card can't be reanimated, until the Cage leaves") {
        val driver = newDriver()
        val me = driver.player1
        val cage = driver.putPermanentOnBattlefield(me, "Grafdigger's Cage")
        val courser = driver.putCardInGraveyard(me, "Centaur Courser")

        driver.giveMana(me, Color.BLACK, 8)
        val first = driver.putCardInHand(me, "Zombify")
        driver.castSpellWithTargets(me, first, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD))).error shouldBe null
        driver.resolveStack()

        withClue("the lock wins over Zombify's instruction; the card stays in the graveyard") {
            driver.getGraveyard(me) shouldContain courser
            driver.findPermanent(me, "Centaur Courser") shouldBe null
        }

        driver.moveToGraveyard(cage)
        val second = driver.putCardInHand(me, "Zombify")
        driver.castSpellWithTargets(me, second, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD))).error shouldBe null
        driver.resolveStack()

        driver.findPermanent(me, "Centaur Courser") shouldBe courser
    }

    test("manifesting a land from the library is impossible: it would enter as a 2/2 creature") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Grafdigger's Cage")
        val top = driver.putCardOnTopOfLibrary(me, "Forest")

        val spell = driver.putCardInHand(me, "Manifest Top")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell).error shouldBe null
        driver.resolveStack()

        driver.getPermanents(me) shouldNotContain top
        driver.state.getZone(ZoneKey(me, Zone.LIBRARY)) shouldContain top
    }

    test("a spell can't be cast from a graveyard; the same spell from hand still can") {
        val driver = newDriver()
        val me = driver.player1
        driver.giveMana(me, Color.BLUE, 4)
        val flashback = driver.putCardInGraveyard(me, "Think Twice")
        val fromHand = driver.putCardInHand(me, "Think Twice")

        driver.castableCards(me) shouldContain flashback
        val flashbackCast = driver.legalActions(me).map { it.action }
            .filterIsInstance<CastSpell>().first { it.cardId == flashback }

        driver.putPermanentOnBattlefield(me, "Grafdigger's Cage")
        val locked = driver.castableCards(me)
        locked shouldNotContain flashback
        locked shouldContain fromHand
        driver.submitExpectFailure(flashbackCast)
    }
})
