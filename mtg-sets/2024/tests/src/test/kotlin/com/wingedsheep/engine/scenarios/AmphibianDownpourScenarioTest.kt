package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.AmphibianDownpour
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Amphibian Downpour (MH3 #51) — {2}{U} Aura with flash and storm. Each storm copy is an Aura spell
 * that may choose a new creature to enchant, and resolves into a token Aura (CR 707.10f).
 */
class AmphibianDownpourScenarioTest : FunSpec({

    val flyingDragon = CardDefinition.creature(
        name = "Flying Dragon",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.FLYING)
    )

    fun setup(spellsCastBefore: Int): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(flyingDragon, AmphibianDownpour))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        driver.replaceState(driver.state.copy(spellsCastThisTurn = spellsCastBefore))
        driver.giveMana(caster, Color.BLUE, 1)
        driver.giveColorlessMana(caster, 2)
        return Triple(driver, caster, driver.getOpponent(caster))
    }

    fun downpours(driver: GameTestDriver): List<EntityId> = driver.state.getBattlefield().filter {
        driver.state.getEntity(it)?.get<CardComponent>()?.name == "Amphibian Downpour"
    }

    fun GameTestDriver.shouldBeFrog(creature: EntityId) {
        val projected = state.projectedState
        projected.getPower(creature) shouldBe 1
        projected.getToughness(creature) shouldBe 1
        projected.hasKeyword(creature, Keyword.FLYING) shouldBe false
        projected.hasSubtype(creature, "Frog") shouldBe true
        projected.hasSubtype(creature, "Dragon") shouldBe false
        projected.getColors(creature) shouldBe setOf(Color.BLUE.name)
    }

    test("enchanted creature loses all abilities and is a blue 1/1 Frog") {
        val (driver, caster, opponent) = setup(spellsCastBefore = 0)
        val dragon = driver.putCreatureOnBattlefield(opponent, "Flying Dragon")
        val aura = driver.putCardInHand(caster, "Amphibian Downpour")

        driver.castSpell(caster, aura, listOf(dragon)).outcome shouldBe Outcome.Done
        // Storm triggers even when it copies zero times; resolve it, then the Aura.
        driver.bothPass()
        driver.bothPass()

        driver.shouldBeFrog(dragon)
        downpours(driver).single().let { driver.state.getEntity(it)!!.has<TokenComponent>() shouldBe false }
    }

    test("a storm copy may enchant a different creature and resolves as a token Aura") {
        val (driver, caster, opponent) = setup(spellsCastBefore = 1)
        val first = driver.putCreatureOnBattlefield(opponent, "Flying Dragon")
        val second = driver.putCreatureOnBattlefield(opponent, "Flying Dragon")
        val aura = driver.putCardInHand(caster, "Amphibian Downpour")

        driver.castSpell(caster, aura, listOf(first)).outcome shouldBe Outcome.Done

        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 5) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(caster, listOf(second)).outcome shouldBe Outcome.Done

        // The copy resolves first, then the original.
        while (driver.state.stack.isNotEmpty() && guard < 10) {
            driver.bothPass()
            guard++
        }

        driver.shouldBeFrog(first)
        driver.shouldBeFrog(second)

        val onBattlefield = downpours(driver)
        onBattlefield.size shouldBe 2
        onBattlefield.map { driver.state.getEntity(it)!!.get<AttachedToComponent>()?.targetId } shouldContainExactlyInAnyOrder
            listOf(first, second)
        val token = onBattlefield.single { driver.state.getEntity(it)!!.has<TokenComponent>() }
        driver.state.getEntity(token)!!.get<AttachedToComponent>()?.targetId shouldBe second
        driver.state.getEntity(aura)!!.get<AttachedToComponent>()?.targetId shouldBe first
    }
})
