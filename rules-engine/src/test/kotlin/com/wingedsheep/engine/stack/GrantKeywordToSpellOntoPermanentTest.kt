package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `GrantKeywordToSpellEffect` on a permanent spell — "whenever you cast a creature spell, it gains
 * haste until end of turn" (Tyvar Kell's emblem). An effect that gives a permanent spell an ability
 * keeps applying to the permanent the spell becomes (CR 400.7a), for the effect's duration; the
 * grant is frozen onto the stack object and
 * handed over as it enters, so a spell that never resolves carries nothing on (CR 400.7: a card
 * that changes zones is a new object).
 */
class GrantKeywordToSpellOntoPermanentTest : FunSpec({

    val projector = StateProjector()

    fun banner(name: String, duration: Duration?) = card(name) {
        manaCost = "{G}"
        typeLine = "Enchantment"
        oracleText = "Whenever you cast a creature spell, it gains haste."
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Creature)
            effect = if (duration == null) Effects.GrantKeywordToSpell(Keyword.HASTE)
            else Effects.GrantKeywordToSpell(Keyword.HASTE, EffectTarget.TriggeringEntity, duration)
        }
    }

    val endOfTurnBanner = banner("Test End-of-Turn Banner", Duration.EndOfTurn)
    /** No printed end: "it gains haste" — the default duration. */
    val permanentBanner = banner("Test Permanent Banner", null)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(endOfTurnBanner, permanentBanner))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    /** Cast Centaur Courser with [banner] out and resolve the cast trigger, leaving the spell on the stack. */
    fun castCourserUnder(driver: GameTestDriver, you: EntityId, banner: String): EntityId {
        driver.putPermanentOnBattlefield(you, banner)
        val courser = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, courser).error shouldBe null
        driver.bothPass() // the cast trigger resolves; the Courser are still on the stack
        driver.state.stack.contains(courser) shouldBe true
        return courser
    }

    test("an until-end-of-turn grant carries onto the permanent and lets it attack at once") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = castCourserUnder(driver, you, "Test End-of-Turn Banner")
        driver.state.getEntity(courser)?.get<SpellGrantedKeywordsComponent>()?.keywords shouldBe setOf("HASTE")
        driver.bothPass() // the Courser resolve

        projector.project(driver.state).hasKeyword(courser, Keyword.HASTE) shouldBe true
        // The grant is credited to what granted it, not to the creature.
        driver.state.floatingEffects.single { courser in it.effect.affectedEntities }.sourceName shouldBe
            "Test End-of-Turn Banner"
        // The stack-only stamp doesn't follow the card onto the battlefield.
        driver.state.getEntity(courser)?.has<SpellGrantedKeywordsComponent>() shouldBe false

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn
        driver.declareAttackers(you, listOf(courser), opponent).error shouldBe null

        driver.passPriorityUntil(Step.UPKEEP)
        (driver.state.turnNumber > turn) shouldBe true
        projector.project(driver.state).hasKeyword(courser, Keyword.HASTE) shouldBe false
    }

    test("a grant with no printed end stays on the permanent past the turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = castCourserUnder(driver, you, "Test Permanent Banner")
        driver.bothPass()

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.UPKEEP)
        (driver.state.turnNumber > turn) shouldBe true
        projector.project(driver.state).hasKeyword(courser, Keyword.HASTE) shouldBe true
    }

    test("a countered spell leaves no grant behind for the card in its graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = castCourserUnder(driver, you, "Test End-of-Turn Banner")
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(courser)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        driver.getGraveyardCardNames(you).contains("Centaur Courser") shouldBe true
        driver.state.floatingEffects.none { courser in it.effect.affectedEntities } shouldBe true
    }
})
