package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RoxanneStarfallSavant
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Roxanne, Starfall Savant (OTJ) — {3}{R}{G} 4/3 Cat Druid.
 *
 * "Whenever Roxanne enters or attacks, create a tapped colorless artifact token named Meteorite
 *  with 'When this token enters, it deals 2 damage to any target' and '{T}: Add one mana of any color.'
 *  Whenever you tap an artifact token for mana, add one mana of any type that artifact token produced."
 *
 * Two triggered abilities (enters / attacks) each create a tapped [PredefinedTokens.Meteorite]; the
 * Meteorite's own ETB-deal-2 and mana ability live on its token definition. The second clause is the
 * [com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap] mirror static over artifact tokens.
 */
class RoxanneStarfallSavantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RoxanneStarfallSavant, PredefinedTokens.Meteorite))
        driver.initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.meteorites(playerId: EntityId): List<EntityId> =
        getPermanents(playerId).filter { getCardName(it) == "Meteorite" }

    test("entering creates a tapped Meteorite whose ETB deals 2 damage to any target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val roxanne = driver.putCardInHand(me, "Roxanne, Starfall Savant")
        driver.giveColorlessMana(me, 3)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, roxanne).isSuccess shouldBe true

        // Drain priority passes / the Meteorite's ETB-damage target choice until the dust settles:
        // Roxanne resolves → "enters" trigger → Meteorite created → its "deals 2 damage to any
        // target" trigger asks for a target, which we point at the opponent.
        repeat(8) {
            val decision = driver.pendingDecision
            if (decision is ChooseTargetsDecision) {
                driver.submitTargetSelection(me, listOf(opp))
            } else {
                driver.bothPass()
            }
        }

        driver.meteorites(me).size shouldBe 1
        driver.getLifeTotal(opp) shouldBe 18
    }

    test("attacking with Roxanne creates another Meteorite") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Put Roxanne onto the battlefield directly (skip the enters trigger for this test by
        // resolving it without acting beyond the forced target choice).
        driver.putCreatureOnBattlefield(me, "Roxanne, Starfall Savant")

        // Advance to combat and attack with Roxanne.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val roxanne = driver.getPermanents(me).first { driver.getCardName(it) == "Roxanne, Starfall Savant" }
        driver.declareAttackers(me, listOf(roxanne), opp)

        // Drain the "attacks" trigger → Meteorite creation → its ETB-damage target choice.
        repeat(8) {
            val decision = driver.pendingDecision
            if (decision is ChooseTargetsDecision) {
                driver.submitTargetSelection(me, listOf(opp))
            } else {
                driver.bothPass()
            }
        }

        driver.meteorites(me).size shouldBe 1
    }

    test("tapping the Meteorite for mana doubles it via Roxanne's mirror ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Roxanne must be on the battlefield (the mirror static is hers), and the Meteorite must be a
        // real artifact *token* (the mirror filter requires `.token()`), so create it via Roxanne's
        // enters trigger rather than a direct placement. Then untap it so it can be tapped for mana.
        val roxanne = driver.putCardInHand(me, "Roxanne, Starfall Savant")
        driver.giveColorlessMana(me, 3)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, roxanne).isSuccess shouldBe true
        repeat(8) {
            val decision = driver.pendingDecision
            if (decision is ChooseTargetsDecision) {
                driver.submitTargetSelection(me, listOf(opp))
            } else {
                driver.bothPass()
            }
        }

        val meteorite = driver.meteorites(me).first()
        driver.untapPermanent(meteorite)
        val manaAbilityId = PredefinedTokens.Meteorite.activatedAbilities[0].id

        // Activating the "{T}: add one mana of any color" ability pauses for the color choice
        // (so the result is "paused", not an error).
        val result = driver.submit(ActivateAbility(playerId = me, sourceId = meteorite, abilityId = manaAbilityId))
        withClue("activation error: ${result.error}") { result.error shouldBe null }

        // Choose blue for the Meteorite's own mana; Roxanne's mirror then adds a second blue of the
        // same type. (There may be a second color prompt if the mirror itself asks — answer blue too.)
        repeat(4) {
            val decision = driver.pendingDecision ?: return@repeat
            driver.submitDecision(me, ColorChosenResponse(decision.id, Color.BLUE))
        }

        // One blue from the Meteorite + one mirrored blue from Roxanne = 2 blue.
        val pool = driver.state.getEntity(me)?.get<ManaPoolComponent>()!!
        pool.blue shouldBe 2
    }
})
