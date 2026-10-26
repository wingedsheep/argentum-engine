package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onc.cards.IxhelScionOfAtraxa
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ixhel, Scion of Atraxa (ONC #1) — {1}{W}{B}{G} Legendary Creature — Phyrexian Angel 2/5.
 *
 * "Corrupted — At the beginning of your end step, each opponent who has three or more poison
 *  counters exiles the top card of their library face down. You may look at and play those cards
 *  for as long as they remain exiled, and you may spend mana as though it were mana of any color to
 *  cast those spells."
 *
 * Three seats: the per-opponent rebind must test each opponent's own poison, exile from *their*
 * library, and still hand the play permission to Ixhel's controller rather than the visited opponent.
 */
class IxhelScionOfAtraxaScenarioTest : FunSpec({

    test("only an opponent with three or more poison exiles their top card, and Ixhel's controller may play it") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IxhelScionOfAtraxa))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, corrupted, belowThreshold) = players
        driver.addComponent(corrupted, CountersComponent(mapOf(CounterType.POISON to 3)))
        driver.addComponent(belowThreshold, CountersComponent(mapOf(CounterType.POISON to 2)))

        driver.putPermanentOnBattlefield(you, "Ixhel, Scion of Atraxa")
        val corruptedTop = driver.putCardOnTopOfLibrary(corrupted, "Grizzly Bears")
        val safeTop = driver.putCardOnTopOfLibrary(belowThreshold, "Grizzly Bears")

        driver.passPriorityUntil(Step.END)
        var steps = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && steps++ < 20) {
            if (driver.pendingDecision != null) driver.autoResolveDecision()
            else driver.passPriority(driver.state.priorityPlayerId!!)
        }

        withClue("the corrupted opponent's top card is exiled face down into their own exile") {
            driver.getExile(corrupted) shouldContain corruptedTop
            driver.state.getEntity(corruptedTop)?.get<FaceDownComponent>().shouldNotBeNull()
        }
        withClue("the opponent below three poison keeps their top card") {
            driver.state.getZone(belowThreshold, Zone.LIBRARY).first() shouldBe safeTop
            driver.getExile(belowThreshold) shouldNotContain safeTop
        }

        val permission = driver.state.mayPlayPermissions.single { corruptedTop in it.cardIds }
        withClue("the permission belongs to Ixhel's controller, not to the opponent visited by the loop") {
            permission.controllerId shouldBe you
        }
        withClue("'for as long as they remain exiled', spending mana as though it were any color") {
            permission.permanent shouldBe true
            permission.withAnyManaColor shouldBe true
        }
    }
})
