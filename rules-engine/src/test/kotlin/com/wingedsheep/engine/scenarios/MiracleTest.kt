package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.MiracleWindowComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.LoreholdTheHistorian
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Miracle (CR 702.94): "You may cast this card for its miracle cost when you draw it if it's the
 * first card you drew this turn."
 *
 * Lorehold, the Historian grants miracle {2} to instant and sorcery cards in its controller's hand
 * (GrantMiracleToCardsInHand). The miracle window opens only on the first card drawn each turn and
 * is cleared at end of turn.
 */
class MiracleTest : FunSpec({

    // A 0-cost sorcery that draws a card — used to draw the miracle card as the first draw.
    val drawOne = card("Miracle Draw Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    test("Granted miracle opens a window on the first card drawn and offers a {2} cast") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoreholdTheHistorian, drawOne))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Lorehold, the Historian")
        // Shock (an instant) on top of the library, drawn as the first card this turn.
        val shock = driver.putCardOnTopOfLibrary(caster, "Shock")
        // Two Mountains for the {2} miracle cost; a 0-cost draw spell to trigger the draw.
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putLandOnBattlefield(caster, "Mountain")
        val drawSpell = driver.putCardInHand(caster, "Miracle Draw Test")

        // Cast the draw spell; Shock is drawn as the first card of the turn.
        driver.castSpell(caster, drawSpell).isSuccess shouldBe true
        driver.bothPass() // resolve the draw

        // The drawn Shock is in hand with an open miracle window.
        (shock in driver.state.getZone(ZoneKey(caster, Zone.HAND))) shouldBe true
        driver.state.getEntity(shock)?.get<MiracleWindowComponent>() shouldNotBe null

        // The enumerator offers a Miracle cast for Shock at the {2} miracle cost.
        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val miracleCast = actions.firstOrNull {
            it.actionType == "CastWithAlternativeCost" && it.description.contains("Miracle", ignoreCase = true)
        }
        miracleCast shouldNotBe null
    }

    test("Casting for the miracle cost pays {2} and resolves the spell") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoreholdTheHistorian, drawOne))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.putPermanentOnBattlefield(caster, "Lorehold, the Historian")
        val shock = driver.putCardOnTopOfLibrary(caster, "Shock")
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.putLandOnBattlefield(caster, "Mountain")
        val drawSpell = driver.putCardInHand(caster, "Miracle Draw Test")

        driver.castSpell(caster, drawSpell).isSuccess shouldBe true
        driver.bothPass()

        val oppLifeBefore = driver.state.getEntity(opponent)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20

        // Cast Shock from hand for its miracle cost ({2}), targeting the opponent.
        val result = driver.submit(
            CastSpell(
                playerId = caster,
                cardId = shock,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(opponent)),
                useAlternativeCost = true,
                alternativeCostType = com.wingedsheep.engine.core.AlternativeCostType.MIRACLE
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve Shock

        val oppLifeAfter = driver.state.getEntity(opponent)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20
        oppLifeAfter shouldBe oppLifeBefore - 2
    }

    test("A second card drawn the same turn does NOT get a miracle window") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoreholdTheHistorian, drawOne))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Lorehold, the Historian")
        // First draw will be a Mountain (no miracle); the SECOND draw is the Shock.
        val shockSecond = driver.putCardOnTopOfLibrary(caster, "Shock")
        driver.putCardOnTopOfLibrary(caster, "Mountain") // now on top, drawn first
        driver.putLandOnBattlefield(caster, "Mountain")
        val draw1 = driver.putCardInHand(caster, "Miracle Draw Test")
        val draw2 = driver.putCardInHand(caster, "Miracle Draw Test")

        driver.castSpell(caster, draw1).isSuccess shouldBe true
        driver.bothPass() // draws Mountain (first draw)
        driver.castSpell(caster, draw2).isSuccess shouldBe true
        driver.bothPass() // draws Shock (second draw)

        // Shock was the second draw → no miracle window.
        driver.state.getEntity(shockSecond)?.get<MiracleWindowComponent>() shouldBe null
    }

    test("Miracle is not offered for a creature card (grant filter is instant/sorcery only)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LoreholdTheHistorian, drawOne))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Lorehold, the Historian")
        val bears = driver.putCardOnTopOfLibrary(caster, "Grizzly Bears") // a creature, not inst/sorc
        driver.putLandOnBattlefield(caster, "Mountain")
        val drawSpell = driver.putCardInHand(caster, "Miracle Draw Test")

        driver.castSpell(caster, drawSpell).isSuccess shouldBe true
        driver.bothPass()

        // No miracle window on a creature card; the grant filter excludes it.
        driver.state.getEntity(bears)?.get<MiracleWindowComponent>() shouldBe null
    }
})
