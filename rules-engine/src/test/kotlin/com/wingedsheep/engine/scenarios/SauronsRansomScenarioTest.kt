package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.SauronsRansom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sauron's Ransom: choose an opponent; they split your top four into two piles;
 * you put one pile into your hand and the other into your graveyard; the Ring tempts you.
 *
 * Exercises the shared Fact-or-Fiction pile-split primitive
 * ([com.wingedsheep.sdk.dsl.Patterns.Library.factOrFiction], count = 4) chained
 * with TheRingTemptsYou.
 */
class SauronsRansomScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SauronsRansom)
        return driver
    }

    test("opponent splits top four; controller routes piles to hand and graveyard; Ring tempts") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack a known top four (top first as pushed last).
        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        // Library from top: c4, c3, c2, c1, <deck...>

        // A creature to designate as Ring-bearer when the Ring tempts.
        val bearer = driver.putCreatureOnBattlefield(active, "Grizzly Bears")

        val spell = driver.putCardInHand(active, "Sauron's Ransom")
        driver.giveMana(active, Color.BLUE, 2)
        driver.giveMana(active, Color.BLACK, 1)

        val handBefore = driver.getHandSize(active)
        val graveBefore = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)).size

        driver.castSpell(active, spell).isSuccess shouldBe true
        driver.bothPass()

        // 1. Opponent separates the revealed top four into two piles.
        driver.isPaused shouldBe true
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe opponent
        split.options.size shouldBe 4
        // Opponent puts c4 and c3 in Pile 1 (selected); c2 and c1 form Pile 2 (remainder).
        driver.submitDecision(opponent, CardsSelectedResponse(split.id, listOf(c4, c3)))

        // 2. Controller chooses which pile goes to hand vs graveyard.
        driver.isPaused shouldBe true
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe active
        // Pile 1 (option 0 = c4, c3) is the "keep" pile → goes to hand; the other → graveyard.
        driver.submitDecision(active, OptionChosenResponse(choose.id, 0))

        // 3. The Ring tempts you → choose a Ring-bearer.
        driver.isPaused shouldBe true
        val tempt = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        tempt.playerId shouldBe active
        driver.submitDecision(active, CardsSelectedResponse(tempt.id, listOf(bearer)))

        driver.isPaused shouldBe false

        // Pile 1 (c4, c3) went to hand; Pile 2 (c2, c1) went to graveyard.
        val hand = driver.state.getZone(ZoneKey(active, Zone.HAND))
        hand.contains(c4) shouldBe true
        hand.contains(c3) shouldBe true

        val grave = driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD))
        grave.contains(c2) shouldBe true
        grave.contains(c1) shouldBe true

        // Hand: before - 1 (spell cast) + 2 (kept pile).
        driver.getHandSize(active) shouldBe handBefore - 1 + 2
        // Graveyard: 2 discarded cards + the Sauron's Ransom spell itself.
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)).size shouldBe graveBefore + 2 + 1

        // The Ring tempted the controller exactly once.
        driver.state.getEntity(active)?.get<TheRingComponent>()?.temptCount shouldBe 1
    }
})
