package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BilbosGambit
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bilbo's Gambit (HOB #5) — {1}{W} Instant.
 *
 *   Gift a Treasure
 *   Return target spell to its owner's hand. If the gift was promised, players can't cast spells
 *   this turn.
 *
 * Mode 0 (no gift): bounce only.
 * Mode 1 (gift): the promised opponent gets a Treasure *before* the other effects, the spell is
 * bounced, and then **every** player — the caster included — is locked out of casting for the turn.
 */
class BilbosGambitScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(BilbosGambit)
        return driver
    }

    test("mode 0 (no gift): the spell is bounced and nobody is locked out") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The caster's own creature spell is the thing being bounced — targeting a spell only needs
        // one on the stack, and using the caster's keeps priority in the right place.
        val bears = driver.putCardInHand(caster, "Grizzly Bears")
        driver.giveMana(caster, Color.GREEN, 2)
        driver.submit(CastSpell(playerId = caster, cardId = bears)).isSuccess shouldBe true

        val gambit = driver.putCardInHand(caster, "Bilbo's Gambit")
        driver.giveMana(caster, Color.WHITE, 2)
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = gambit,
                targets = listOf(ChosenTarget.Spell(bears)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(bears)))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        (bears in driver.getHand(caster)) shouldBe true
        driver.findPermanent(opponent, "Treasure").shouldBeNull()
        driver.state.getEntity(caster)?.get<CantCastSpellsComponent>().shouldBeNull()
        driver.state.getEntity(opponent)?.get<CantCastSpellsComponent>().shouldBeNull()
    }

    test("mode 1 (gift): the opponent gets the Treasure and EVERY player is locked out this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCardInHand(caster, "Grizzly Bears")
        driver.giveMana(caster, Color.GREEN, 2)
        driver.submit(CastSpell(playerId = caster, cardId = bears)).isSuccess shouldBe true

        val gambit = driver.putCardInHand(caster, "Bilbo's Gambit")
        driver.giveMana(caster, Color.WHITE, 2)
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = gambit,
                targets = listOf(ChosenTarget.Spell(bears)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(bears)))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        (bears in driver.getHand(caster)) shouldBe true

        // The gift goes to the promised opponent, never the caster.
        driver.findPermanent(opponent, "Treasure").shouldNotBeNull()
        driver.findPermanent(caster, "Treasure").shouldBeNull()

        // "Players" is everyone — the caster is shut off too.
        driver.state.getEntity(caster)?.get<CantCastSpellsComponent>().shouldNotBeNull()
        driver.state.getEntity(opponent)?.get<CantCastSpellsComponent>().shouldNotBeNull()

        // And it bites: the just-bounced Bears can't be recast this turn.
        driver.giveMana(caster, Color.GREEN, 2)
        driver.submit(CastSpell(playerId = caster, cardId = bears)).isSuccess shouldBe false
    }
})
