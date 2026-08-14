package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.LorwynEclipsedSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 605.3a — "A player may activate an activated mana ability whenever they have priority,
 * whenever they are casting a spell or activating an ability that requires a mana payment, **or
 * whenever a rule or effect asks for a mana payment**."
 *
 * That last clause is what these tests cover. A [SelectManaSourcesDecision] is the engine asking a
 * player for mana (ward, "you may pay {B}", an attack tax); the player holds no priority while it
 * is open, but they may still activate mana abilities to produce what they need.
 *
 * Before this, the decision's pre-computed `availableSources` menu was the only way to make mana
 * during a payment — and [com.wingedsheep.engine.mechanics.mana.ManaSolver.findAvailableManaSources]
 * only models `{T}`-shaped abilities, so anything else (Ashnod's Altar's "Sacrifice a creature: Add
 * {C}{C}") was simply unreachable.
 */
class ManaAbilitiesDuringPaymentTest : FunSpec({

    /**
     * An Ashnod's-Altar-shaped mana source: the activation cost has no `{T}`, so the solver skips
     * it and it never appears in a [SelectManaSourcesDecision]'s source menu.
     */
    val bloodChalice = card("Blood Chalice") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.AddMana(Color.BLACK)
            manaAbility = true
        }
    }

    /** A plain non-mana activated ability — must stay locked while a payment window is open. */
    val tomeOfIdeas = card("Tome of Ideas") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.DrawCards(1)
        }
    }

    val wardedBear = card("Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.ward("{1}"))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LorwynEclipsedSet.cards + listOf(bloodChalice, tomeOfIdeas, wardedBear))
        return driver
    }

    fun GameTestDriver.manaPool(playerId: com.wingedsheep.sdk.model.EntityId): ManaPoolComponent =
        state.getEntity(playerId)!!.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.manaAbilityOf(cardName: String) =
        cardRegistry.getCard(cardName)!!.script.activatedAbilities.first { it.isManaAbility }

    /** Advance to the controller's next first main phase so Eirdu's may-pay trigger fires. */
    fun GameTestDriver.advanceToNextFirstMain() {
        passPriorityUntil(Step.END)
        bothPass()
        passPriorityUntil(Step.END)
        bothPass()
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Eirdu's first-main trigger, answered "yes", leaves a mana-source decision open. */
    fun openEirduPaymentWindow(driver: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val eirdu = driver.putCreatureOnBattlefield(caster, "Eirdu, Carrier of Dawn")
        val birds = driver.putCreatureOnBattlefield(caster, "Birds of Paradise")
        driver.advanceToNextFirstMain()
        driver.bothPass()
        driver.submitYesNo(caster, true)
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        return eirdu to birds
    }

    test("mana ability can be activated while a may-pay mana source decision is open") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true)
        val caster = driver.activePlayer!!
        val (eirdu, birds) = openEirduPaymentWindow(driver)

        val result = driver.submit(
            ActivateAbility(caster, birds, driver.manaAbilityOf("Birds of Paradise").id, manaColorChoice = Color.BLACK)
        )
        result.error shouldBe null

        // The mana is floating and the window is still open.
        driver.manaPool(caster).black shouldBe 1
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Confirming with nothing selected now pays from the floating mana instead of declining.
        driver.submitDecision(caster, ManaSourcesSelectedResponse(driver.pendingDecision!!.id))
        driver.bothPass()

        driver.state.getEntity(eirdu)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe
            DoubleFacedComponent.Face.BACK
        driver.manaPool(caster).black shouldBe 0
    }

    test("a source tapped by hand drops out of the refreshed source menu") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true)
        val caster = driver.activePlayer!!
        val (_, birds) = openEirduPaymentWindow(driver)

        (driver.pendingDecision as SelectManaSourcesDecision)
            .availableSources.map { it.entityId } shouldContain birds

        driver.submit(
            ActivateAbility(caster, birds, driver.manaAbilityOf("Birds of Paradise").id, manaColorChoice = Color.BLACK)
        ).error shouldBe null

        (driver.pendingDecision as SelectManaSourcesDecision)
            .availableSources.map { it.entityId } shouldNotContain birds
    }

    test("non-mana abilities are still rejected while a payment window is open") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true)
        val caster = driver.activePlayer!!
        openEirduPaymentWindow(driver)

        val tome = driver.putPermanentOnBattlefield(caster, "Tome of Ideas")
        val drawAbility = driver.cardRegistry.getCard("Tome of Ideas")!!
            .script.activatedAbilities.first { !it.isManaAbility }

        driver.submit(ActivateAbility(caster, tome, drawAbility.id))
            .error shouldBe "Only mana abilities can be activated while paying a cost"

        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }

    test("ward is paid with a mana ability the source menu cannot offer") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        val chalice = driver.putPermanentOnBattlefield(caster, "Blood Chalice")
        // A Mountain is what the solver can see; it keeps the ward affordability pre-gate happy.
        driver.putPermanentOnBattlefield(caster, "Mountain")

        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        // "Sacrifice this artifact: Add {B}" has no {T} in its cost, so the solver never models it.
        decision.availableSources.map { it.entityId } shouldNotContain chalice

        driver.submit(ActivateAbility(caster, chalice, driver.manaAbilityOf("Blood Chalice").id))
            .error shouldBe null
        driver.manaPool(caster).black shouldBe 1

        // Confirm with nothing selected — the floating {B} covers ward {1}, so this pays rather
        // than declining, and the bolt is not countered.
        driver.submitDecision(caster, ManaSourcesSelectedResponse(driver.pendingDecision!!.id))
        driver.bothPass()

        driver.findPermanent(opponent, "Warded Bear") shouldBe null
    }

    test("ward prompts at all when the only mana source is one the solver can't tap") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        // No lands at all — Blood Chalice's "Sacrifice this artifact: Add {B}" is the only way to
        // pay ward {1}, and `findAvailableManaSources` cannot see it. Before the affordability
        // path learned about explicit-activation sources, the pre-gate concluded "can't pay" and
        // countered the spell without ever asking.
        val chalice = driver.putPermanentOnBattlefield(caster, "Blood Chalice")

        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.availableSources.shouldBeEmpty()

        driver.submit(ActivateAbility(caster, chalice, driver.manaAbilityOf("Blood Chalice").id))
            .error shouldBe null
        driver.submitDecision(caster, ManaSourcesSelectedResponse(driver.pendingDecision!!.id))
        driver.bothPass()

        driver.findPermanent(opponent, "Warded Bear") shouldBe null
    }

    test("ward still counters with no way at all to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        // A Blood Chalice with nothing to sacrifice… is fine, it sacrifices itself. Give the
        // caster nothing instead: the pre-gate must still short-circuit to countered.
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Warded Bear").shouldNotBeNull()
    }

    test("declining a ward explicitly still counters the spell even with mana floating") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        driver.putPermanentOnBattlefield(caster, "Mountain")

        driver.giveMana(caster, Color.RED, 2)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(bear)))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.canDecline shouldBe true

        // A leftover {R} is floating and would cover ward {1} — only the explicit flag means "no".
        driver.manaPool(caster).red shouldBe 1
        driver.submitDecision(caster, ManaSourcesSelectedResponse(decision.id, declined = true))
        driver.bothPass()

        driver.findPermanent(opponent, "Warded Bear").shouldNotBeNull()
    }

    test("the mana-payment window does not let a player cast spells or hold priority") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true)
        val caster = driver.activePlayer!!
        openEirduPaymentWindow(driver)

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        val error = driver.submit(
            com.wingedsheep.engine.core.CastSpell(caster, bolt)
        ).error
        error.shouldNotBeNull()

        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }
})
