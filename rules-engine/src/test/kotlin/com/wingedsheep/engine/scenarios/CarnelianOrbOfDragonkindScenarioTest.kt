package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.CarnelianOrbOfDragonkind
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Carnelian Orb of Dragonkind (canonical CLB #166, reprinted FDN #759).
 *
 * {T}: Add {R}. If that mana is spent on a Dragon creature spell, it gains haste until end of turn.
 *
 * Covers the new [com.wingedsheep.sdk.scripting.effects.ManaSpellRider.GrantsKeywordWhenSpent]
 * rider — the first rider that applies a continuous effect instead of queuing a trigger. The
 * grant is keyed to the *spell's* entity id while it is still on the stack, so the interesting
 * question these tests answer is whether it survives the stack → battlefield transition and lands
 * on the permanent in time to matter. Haste makes that observable: the assertion is an actual
 * attack the turn the Dragon enters, not just a projected keyword.
 *
 * The negatives pin the filter on both axes the printed rulings call out: card type (a Dragon
 * *token-making sorcery* is not a Dragon creature spell) and subtype (a non-Dragon creature). The
 * last test covers "the mana can be spent on anything" — the rider rides along on every cast the
 * mana pays for and simply no-ops when the spell doesn't match.
 */
class CarnelianOrbOfDragonkindScenarioTest : FunSpec({

    val projector = StateProjector()
    val orbAbilityId = CarnelianOrbOfDragonkind.activatedAbilities[0].id

    /** A {R} Dragon — one Orb activation pays its whole cost. */
    val dragon = CardDefinition.creature(
        name = "Test Ember Wyrm",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 2,
        toughness = 2,
    )

    /** Same cost, same colour, no Dragon subtype — the rider must not fire. */
    val nonDragon = CardDefinition.creature(
        name = "Test Ember Imp",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Imp")),
        power = 2,
        toughness = 2,
    )

    /**
     * "An instant or sorcery spell is not a creature spell, even if that spell creates Dragon
     * creature tokens." A noncreature spell the {R} genuinely pays for, so the rider is consumed
     * and must then no-op.
     */
    val dragonSorcery = CardDefinition.sorcery(
        name = "Test Dragon Call",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "You gain 2 life.",
        script = CardScript.spell(effect = Effects.GainLife(2)),
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(CarnelianOrbOfDragonkind, dragon, nonDragon, dragonSorcery)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Tap the Orb, putting one rider-carrying {R} into the pool. */
    fun tapOrb(driver: GameTestDriver, you: EntityId, orb: EntityId) {
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = orb, abilityId = orbAbilityId)
        )
    }

    test("a Dragon creature spell paid with the Orb's mana has haste and can attack at once") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(you, "Carnelian Orb of Dragonkind")
        val wyrm = driver.putCardInHand(you, "Test Ember Wyrm")
        tapOrb(driver, you, orb)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.bothPass()

        // The grant was floated onto the spell on the stack; a permanent spell keeps its entity id
        // as it resolves, so it is already live on the permanent.
        projector.project(driver.state).hasKeyword(wyrm, Keyword.HASTE) shouldBe true
        driver.state.getEntity(wyrm)?.has<SummoningSicknessComponent>() shouldBe true

        // Pinning the turn is what makes this assertion discriminating: the engine skips the
        // declare-attackers step outright when the active player has no legal attacker, so a
        // Dragon *without* haste would sail past this turn's combat and only be able to attack a
        // turn later. Reaching declare-attackers in the turn it entered is haste doing its job.
        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.state.turnNumber shouldBe turn
        driver.declareAttackers(you, listOf(wyrm), opponent).error shouldBe null
    }

    test("the same Dragon cast with ordinary red mana stays summoning sick") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wyrm = driver.putCardInHand(you, "Test Ember Wyrm")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.bothPass()

        // The control for the test above: same Dragon, same turn, mana from anywhere else. It is
        // summoning sick with no haste to bypass it, so this turn's combat is not open to it.
        projector.project(driver.state).hasKeyword(wyrm, Keyword.HASTE) shouldBe false
        driver.state.getEntity(wyrm)?.has<SummoningSicknessComponent>() shouldBe true
    }

    test("a non-Dragon creature spell paid with the Orb's mana gains nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(you, "Carnelian Orb of Dragonkind")
        val imp = driver.putCardInHand(you, "Test Ember Imp")
        tapOrb(driver, you, orb)

        driver.castSpell(you, imp).error shouldBe null
        driver.bothPass()

        projector.project(driver.state).hasKeyword(imp, Keyword.HASTE) shouldBe false
    }

    test("the mana can be spent on anything — a noncreature spell casts fine and gains nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val orb = driver.putPermanentOnBattlefield(you, "Carnelian Orb of Dragonkind")
        val call = driver.putCardInHand(you, "Test Dragon Call")
        tapOrb(driver, you, orb)

        // The {R} is unrestricted, so it really does pay for this sorcery: the rider is consumed
        // and must no-op rather than error.
        driver.castSpell(you, call).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 22
    }
})
