package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Through the Breach — {4}{R} Instant — Arcane (Champions of Kamigawa; reprinted in Innistrad
 * Remastered).
 *
 * "You may put a creature card from your hand onto the battlefield. That creature gains haste.
 *  Sacrifice that creature at the beginning of the next end step.
 *  Splice onto Arcane {2}{R}{R}"
 *
 * Two halves worth pinning: the cheat-into-play itself (put, haste, delayed sacrifice), and the fact
 * that this same text can instead be *spliced* onto another Arcane spell (CR 702.47) — the card then
 * stays in hand while the spell it was spliced onto does the cheating.
 */
class ThroughTheBreachScenarioTest : FunSpec({

    // A plain Arcane spell to splice Through the Breach onto.
    val arcaneSpark = card("Test Arcane Spark") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        spell {
            val t = target("spark", Targets.Player)
            effect = Effects.DealDamage(1, t)
        }
    }

    // An untargeted splice card to splice onto Through the Breach — which is itself Arcane, so it can
    // be the host as well as the guest.
    val spliceGain = card("Test Splice Gain") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        splice("{1}")
        spell { effect = Effects.GainLife(3) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(arcaneSpark)
        driver.registerCard(spliceGain)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolve Through the Breach and pick [creature] out of hand for its "you may put" choice. */
    fun putFromHand(driver: GameTestDriver, player: EntityId, creature: EntityId) {
        driver.bothPass()
        driver.submitCardSelection(player, listOf(creature))
    }

    test("puts a creature from hand onto the battlefield with haste") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val breach = driver.putCardInHand(player, "Through the Breach")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(player, breach, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        putFromHand(driver, player, fatty)

        val angler = driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()
        // It was put onto the battlefield, not cast — the card left hand for the battlefield.
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldNotBe listOf(fatty)
        // "That creature gains haste" — no duration, so it simply has haste.
        driver.state.projectedState.getKeywords(angler) shouldContain Keyword.HASTE.name
    }

    test("the creature is sacrificed at the beginning of the next end step") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val breach = driver.putCardInHand(player, "Through the Breach")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(player, breach, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        putFromHand(driver, player, fatty)
        driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(player, "Gurmag Angler") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain fatty
    }

    test("declining the optional put is a legal no-op") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val breach = driver.putCardInHand(player, "Through the Breach")
        driver.putCardInHand(player, "Gurmag Angler")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(player, breach, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()
        // "You *may* put" — selecting nothing resolves the spell with nothing happening.
        driver.submitCardSelection(player, emptyList())

        driver.findPermanent(player, "Gurmag Angler") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain breach
    }

    test("spliced onto an Arcane spell it cheats a creature in and stays in hand (CR 702.47)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val spark = driver.putCardInHand(player, "Test Arcane Spark")
        val breach = driver.putCardInHand(player, "Through the Breach")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")
        // {R} for the Spark + {2}{R}{R} splice = five mana, three of them red.
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(
                player, spark,
                targets = listOf(ChosenTarget.Player(opponent)),
                splicedCardIds = listOf(breach),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        putFromHand(driver, player, fatty)

        // The Spark's own damage happened first (CR 702.47b), then the spliced text.
        driver.getLifeTotal(opponent) shouldBe 19
        driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()
        // CR 702.47a — Through the Breach was only revealed; it is still in hand.
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain breach
    }

    test("the spliced creature is sacrificed at the next end step just the same") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val spark = driver.putCardInHand(player, "Test Arcane Spark")
        val breach = driver.putCardInHand(player, "Through the Breach")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(
                player, spark,
                targets = listOf(ChosenTarget.Player(opponent)),
                splicedCardIds = listOf(breach),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        putFromHand(driver, player, fatty)
        driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(player, "Gurmag Angler") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain fatty
        // Still only revealed, even after the whole thing resolved.
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain breach
    }

    test("a splice tail still runs after the main spell pauses for its own decision (CR 702.47b)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Through the Breach as the *host*: its own effect pauses to pick a creature out of hand, so
        // the spliced text has to survive that pause and run once the decision resolves.
        val breach = driver.putCardInHand(player, "Through the Breach")
        val gain = driver.putCardInHand(player, "Test Splice Gain")
        val fatty = driver.putCardInHand(player, "Gurmag Angler")
        // {4}{R} for the Breach + {1} for the splice = six mana.
        driver.giveMana(player, Color.RED, 6)

        driver.submit(
            CastSpell(
                player, breach,
                splicedCardIds = listOf(gain),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null

        driver.bothPass()
        // Mid-resolution: the main spell is waiting on its card selection and has gained no life yet.
        driver.getLifeTotal(player) shouldBe 20
        driver.submitCardSelection(player, listOf(fatty))

        // The creature came in *and* the spliced life gain fired afterwards.
        driver.findPermanent(player, "Gurmag Angler").shouldNotBeNull()
        driver.getLifeTotal(player) shouldBe 23
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain gain
    }

    test("the splice cast variant is offered while an Arcane spell is castable") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Test Arcane Spark")
        driver.putCardInHand(player, "Through the Breach")
        driver.giveMana(player, Color.RED, 5)

        driver.legalActions(player).map { it.description } shouldContain
            "Cast Test Arcane Spark (Splice Through the Breach)"
    }
})
