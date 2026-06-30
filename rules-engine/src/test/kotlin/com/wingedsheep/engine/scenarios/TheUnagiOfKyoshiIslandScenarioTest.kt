package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Divination
import com.wingedsheep.mtg.sets.definitions.tla.cards.TheUnagiOfKyoshiIsland
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for The Unagi of Kyoshi Island ({3}{U}{U}, Legendary Creature — Serpent, 5/5):
 *  - Flash (printed keyword).
 *  - Ward—Waterbend {4}: when an opponent targets it, counter that spell/ability unless the
 *    controller pays {4}. They may tap their untapped artifacts and creatures to help, each
 *    paying {1} — the ward mana payment routes through the shared waterbend tap-to-help path.
 *  - "Whenever an opponent draws their second card each turn, you draw two cards."
 *    (the [com.wingedsheep.sdk.dsl.Triggers.NthCardDrawn] facade scoped to each opponent).
 */
class TheUnagiOfKyoshiIslandScenarioTest : FunSpec({

    // A pure (non-creature) artifact, to prove an artifact can be tapped to help pay the ward.
    val trinket = card("Unagi Test Trinket") {
        manaCost = "{1}"
        colorIdentity = ""
        typeLine = "Artifact"
        oracleText = ""
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheUnagiOfKyoshiIsland, Divination, trinket))
        return driver
    }

    fun markedDamage(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("Flash is a printed keyword on The Unagi") {
        TheUnagiOfKyoshiIsland.keywords.contains(Keyword.FLASH) shouldBe true
    }

    test("Ward—Waterbend counters the targeting spell when the controller cannot pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val owner = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val unagi = driver.putCreatureOnBattlefield(owner, "The Unagi of Kyoshi Island")

        // Caster has exactly {R} for the Bolt — nothing to pay the {4} ward, and no
        // artifacts/creatures to waterbend-tap, so the executor counters immediately.
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(unagi)).isSuccess shouldBe true

        // Resolve the ward trigger — caster can't pay, so it counters straight away.
        driver.bothPass()
        driver.pendingDecision shouldBe null

        // Bolt was countered before dealing damage; The Unagi is unharmed.
        driver.findPermanent(owner, "The Unagi of Kyoshi Island") shouldNotBe null
        markedDamage(driver, unagi) shouldBe 0
        driver.getGraveyardCardNames(caster).contains("Lightning Bolt") shouldBe true
    }

    test("paying the {4} ward with mana lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val owner = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val unagi = driver.putCreatureOnBattlefield(owner, "The Unagi of Kyoshi Island")

        // Four untapped Islands cover the {4} ward; {R} (floating) covers the Bolt.
        repeat(4) { driver.putLandOnBattlefield(caster, "Island") }
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(unagi))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Pay the {4} ward with mana — the Bolt resolves and marks 3 damage on the 5/5.
        driver.submitManaAutoPayOrDecline(caster, autoPay = true)
        repeat(4) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(owner, "The Unagi of Kyoshi Island") shouldNotBe null
        markedDamage(driver, unagi) shouldBe 3
    }

    test("the {4} ward can be paid by tapping artifacts and creatures (Waterbend)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val owner = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val unagi = driver.putCreatureOnBattlefield(owner, "The Unagi of Kyoshi Island")

        // The caster has only {R} for the Bolt — the {4} ward is covered entirely by tapping two
        // creatures and two artifacts, each paying {1} via the waterbend tap-to-help path.
        driver.giveMana(caster, Color.RED, 1)
        val creature1 = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        val creature2 = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        val artifact1 = driver.putPermanentOnBattlefield(caster, "Unagi Test Trinket")
        val artifact2 = driver.putPermanentOnBattlefield(caster, "Unagi Test Trinket")
        val taps = setOf(creature1, creature2, artifact1, artifact2)

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(unagi))

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        // The ward payment surfaces every untapped artifact/creature the caster controls.
        decision.waterbendPermanents.map { it.entityId }.toSet() shouldBe taps

        // Tap all four permanents to pay the {4} — no mana sources selected.
        driver.submitDecision(
            caster,
            ManaSourcesSelectedResponse(
                decisionId = decision.id,
                selectedSources = emptyList(),
                autoPay = false,
                waterbendPermanents = taps
            )
        )
        repeat(4) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // The ward was paid, so the Bolt resolved and marked 3 damage on the 5/5.
        markedDamage(driver, unagi) shouldBe 3
        taps.all { driver.state.getEntity(it)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() } shouldBe true
    }

    test("declining the {4} ward counters the targeting spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val owner = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val unagi = driver.putCreatureOnBattlefield(owner, "The Unagi of Kyoshi Island")

        repeat(4) { driver.putLandOnBattlefield(caster, "Island") }
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(unagi))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Decline — the Bolt is countered and never marks damage.
        driver.submitManaAutoPayOrDecline(caster, autoPay = false)
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(owner, "The Unagi of Kyoshi Island") shouldNotBe null
        markedDamage(driver, unagi) shouldBe 0
    }

    test("you draw two cards when an opponent draws their second card each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        // The active player (p1) is the opponent that will draw; p2 controls The Unagi.
        val drawer = driver.activePlayer!!
        val observer = driver.getOpponent(drawer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(observer, "The Unagi of Kyoshi Island")

        // Turn 1's active player skips the draw step, so Divination draws the drawer's 1st and
        // 2nd cards — the 2nd crosses N=2 and fires The Unagi's trigger for its controller.
        val divination = driver.putCardInHand(drawer, "Divination")
        driver.giveMana(drawer, Color.BLUE, 1)
        driver.giveColorlessMana(drawer, 2)

        val observerHandBefore = driver.getHand(observer).size
        driver.castSpell(drawer, divination)
        repeat(6) {
            if (driver.pendingDecision != null || driver.state.stack.isNotEmpty()) driver.bothPass()
        }

        // The Unagi's controller drew two cards off the opponent's second draw.
        driver.getHand(observer).size shouldBe observerHandBefore + 2
    }
})
