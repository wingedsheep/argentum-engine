package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Tests for Glarb, Calamity's Augur.
 *
 * Glarb, Calamity's Augur: {B}{G}{U}
 * Legendary Creature — Frog Wizard Noble
 * 2/4
 * Deathtouch
 * You may look at the top card of your library any time.
 * You may play lands and cast spells with mana value 4 or greater from the top of your library.
 * {T}: Surveil 2.
 */
class GlarbCalamitysAugurTest : FunSpec({

    val GlarbCard = card("Glarb, Calamity's Augur") {
        manaCost = "{B}{G}{U}"
        typeLine = "Legendary Creature — Frog Wizard Noble"
        power = 2
        toughness = 4

        keywords(Keyword.DEATHTOUCH)

        staticAbility {
            ability = LookAtTopOfLibrary
        }

        staticAbility {
            ability = PlayLandsAndCastFilteredFromTopOfLibrary(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4)
            )
        }

        activatedAbility {
            cost = AbilityCost.Tap
            effect = LibraryPatterns.surveil(2)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GlarbCard)
        return driver
    }

    test("can play a land from top of library with Glarb") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Glarb, Calamity's Augur")

        val forestOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Forest")

        val playResult = driver.playLand(activePlayer, forestOnTop)
        playResult.isSuccess shouldBe true

        driver.findPermanent(activePlayer, "Forest") shouldNotBe null
    }

    test("can cast spell with mana value 4 or greater from top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Glarb, Calamity's Augur")

        // Frogmite has MV 4
        val frogmiteOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Frogmite")

        driver.giveMana(activePlayer, Color.GREEN, 4)

        val castResult = driver.castSpell(activePlayer, frogmiteOnTop)
        castResult.isSuccess shouldBe true

        driver.bothPass()

        driver.findPermanent(activePlayer, "Frogmite") shouldNotBe null
    }

    test("cannot cast spell with mana value less than 4 from top of library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Glarb, Calamity's Augur")

        // Lightning Bolt has MV 1
        val boltOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Lightning Bolt")

        driver.giveMana(activePlayer, Color.RED, 1)

        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val castResult = driver.castSpell(activePlayer, boltOnTop, listOf(creature))
        castResult.isSuccess shouldBe false
    }

    test("surveil 2 activated ability works") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glarb = driver.putPermanentOnBattlefield(activePlayer, "Glarb, Calamity's Augur")
        driver.removeSummoningSickness(glarb)

        // Put known cards on top of library
        val card1 = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val card2 = driver.putCardOnTopOfLibrary(activePlayer, "Island")

        // Activate tap ability for surveil 2
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = glarb,
                abilityId = GlarbCard.script.activatedAbilities.first().id
            )
        )
        activateResult.isSuccess shouldBe true
        driver.bothPass()

        // Should be paused for card selection (select cards to put in graveyard)
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options.size shouldBe 2

        // Select both cards to put in graveyard
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(
                decisionId = decision.id,
                selectedCards = listOf(card2, card1)
            )
        )

        // Both cards should be in graveyard
        val graveyardZone = ZoneKey(activePlayer, Zone.GRAVEYARD)
        val graveyard = driver.state.getZone(graveyardZone)
        graveyard.contains(card1) shouldBe true
        graveyard.contains(card2) shouldBe true
    }
})
