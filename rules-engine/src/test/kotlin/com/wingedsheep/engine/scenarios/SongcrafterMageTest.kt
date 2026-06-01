package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.SongcrafterMage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Songcrafter Mage (Tarkir: Dragonstorm): "When this creature enters, target instant or sorcery
 * card in your graveyard gains harmonize until end of turn. Its harmonize cost is equal to its
 * mana cost." (CR 702.180.)
 *
 * Exercises the runtime "granted harmonize" feature end to end: the ETB grants harmonize to a
 * graveyard spell that otherwise has no harmonize keyword, the cast-from-graveyard enumerator and
 * payment handler honor the grant (cost = the card's mana cost, plus the tap-for-power reduction),
 * the spell is exiled as it resolves, and the grant expires at end of turn.
 */
class SongcrafterMageTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SongcrafterMage)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Songcrafter Mage and resolve its ETB targeting [graveyardSpell]. */
    fun grantHarmonize(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, graveyardSpell: com.wingedsheep.sdk.model.EntityId) {
        val mage = driver.putCardInHand(player, "Songcrafter Mage")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, mage)
        driver.bothPass() // resolve the creature; its ETB trigger goes on the stack
        driver.submitTargetSelection(player, listOf(graveyardSpell))
        driver.bothPass() // resolve the ETB → grant harmonize
    }

    test("granted harmonize lets a graveyard spell be cast for its mana cost, then exiles it") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt") // {R}, deals 3 to any target
        grantHarmonize(driver, player, bolt)

        // Its harmonize cost equals its mana cost: {R}.
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                player, bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(bolt) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(bolt) shouldBe false
    }

    test("tapping a creature reduces the granted harmonize generic cost by its power") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        // Doom Blade {1}{B} → harmonize cost {1}{B}; tapping a 2-power bear covers the {1}.
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val doomBlade = driver.putCardInGraveyard(player, "Doom Blade")
        grantHarmonize(driver, player, doomBlade)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2
        driver.giveMana(player, Color.BLACK, 1) // only {B}; the {1} comes from tapping the bear

        driver.submit(
            CastSpell(
                player, doomBlade,
                targets = listOf(ChosenTarget.Permanent(opponentCreature)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = AlternativePaymentChoice(harmonizeCreature = bears)
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.isTapped(bears) shouldBe true
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).contains(opponentCreature) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(doomBlade) shouldBe true
    }

    test("enumeration surfaces a CastWithHarmonize action for the granted spell") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        grantHarmonize(driver, player, bolt)
        driver.giveMana(player, Color.RED, 1)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)

        val harmonizeAction = actions.firstOrNull { la ->
            la.actionType == "CastWithHarmonize" && (la.action as? CastSpell)?.cardId == bolt
        }
        harmonizeAction shouldNotBe null
        harmonizeAction!!.hasHarmonize shouldBe true
        harmonizeAction.affordable shouldBe true
    }

    test("only the targeted card gains harmonize, not other graveyard spells") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val study = driver.putCardInGraveyard(player, "Careful Study") // not targeted
        grantHarmonize(driver, player, bolt)
        driver.giveMana(player, Color.RED, 2)

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)

        actions.any { (it.action as? CastSpell)?.cardId == bolt && it.actionType == "CastWithHarmonize" } shouldBe true
        actions.any { (it.action as? CastSpell)?.cardId == study && it.actionType == "CastWithHarmonize" } shouldBe false
    }

    test("the granted harmonize expires at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        grantHarmonize(driver, player, bolt)
        driver.state.grantedKeywordAbilities.isNotEmpty() shouldBe true

        // Pass to the next turn — the cleanup step removes the EndOfTurn grant.
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.grantedKeywordAbilities.isEmpty() shouldBe true

        // The spell is no longer castable from the graveyard via harmonize.
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        actions.any { (it.action as? CastSpell)?.cardId == bolt && it.actionType == "CastWithHarmonize" } shouldBe false
    }
})
