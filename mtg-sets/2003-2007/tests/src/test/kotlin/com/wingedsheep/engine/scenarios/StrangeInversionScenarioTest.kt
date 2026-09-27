package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Strange Inversion — {2}{R} Instant — Arcane (Champions of Kamigawa).
 *
 * "Switch target creature's power and toughness until end of turn. Splice onto Arcane {1}{R}"
 */
class StrangeInversionScenarioTest : FunSpec({

    val lopsided = card("Test Lopsided Beast") {
        manaCost = "{2}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 3
    }

    // A plain Arcane spell to splice Strange Inversion onto.
    val arcaneSpark = card("Test Arcane Spark") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant — Arcane"
        spell {
            val t = target(Targets.Player)
            effect = Effects.DealDamage(1, t)
        }
    }

    // A non-Arcane pump to prove the switch applies after later stat changes.
    val pump = card("Test Power Pump") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 0, t)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(lopsided)
        driver.registerCard(arcaneSpark)
        driver.registerCard(pump)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("switches target creature's power and toughness until end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCreatureOnBattlefield(player, "Test Lopsided Beast")
        val inversion = driver.putCardInHand(player, "Strange Inversion")
        driver.giveMana(player, Color.RED, 3)

        driver.submit(
            CastSpell(
                player, inversion,
                targets = listOf(ChosenTarget.Permanent(beast)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        driver.state.projectedState.getPower(beast) shouldBe 3
        driver.state.projectedState.getToughness(beast) shouldBe 1
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)) shouldContain inversion

        // "Until end of turn" — back to 1/3 on the next turn.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getPower(beast) shouldBe 1
        driver.state.projectedState.getToughness(beast) shouldBe 3
    }

    test("the switch applies after a later +2/+0 (1/3 becomes 3/3, not 5/1)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val beast = driver.putCreatureOnBattlefield(player, "Test Lopsided Beast")
        val inversion = driver.putCardInHand(player, "Strange Inversion")
        val pumpCard = driver.putCardInHand(player, "Test Power Pump")
        driver.giveMana(player, Color.RED, 4)

        driver.submit(
            CastSpell(
                player, inversion,
                targets = listOf(ChosenTarget.Permanent(beast)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()
        driver.submit(
            CastSpell(
                player, pumpCard,
                targets = listOf(ChosenTarget.Permanent(beast)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        // 1/3 +2/+0 = 3/3, switched = 3/3.
        driver.state.projectedState.getPower(beast) shouldBe 3
        driver.state.projectedState.getToughness(beast) shouldBe 3
    }

    test("spliced onto an Arcane spell it switches the creature and stays in hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val beast = driver.putCreatureOnBattlefield(player, "Test Lopsided Beast")
        val spark = driver.putCardInHand(player, "Test Arcane Spark")
        val inversion = driver.putCardInHand(player, "Strange Inversion")
        // {R} for the Spark + {1}{R} splice.
        driver.giveMana(player, Color.RED, 3)

        driver.submit(
            CastSpell(
                player, spark,
                targets = listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(beast)),
                splicedCardIds = listOf(inversion),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 19
        driver.state.projectedState.getPower(beast) shouldBe 3
        driver.state.projectedState.getToughness(beast) shouldBe 1
        driver.state.getZone(ZoneKey(player, Zone.HAND)) shouldContain inversion
    }
})
