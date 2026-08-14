package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.EarthKingdomGeneral
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Earth Kingdom General's second ability:
 * "Whenever you put one or more +1/+1 counters on a creature, you may gain that much life.
 *  Do this only once each turn."
 *
 * The oracle recipient is "a creature" (ANY creature, not just yours), so the "you put" scope
 * is carried by the trigger's `placedBy = Player.You` selector (CR 122.6a) rather than a
 * "creature you control" recipient filter. These tests pin the placer distinction — a placement
 * by *you* fires it, a placement by an *opponent* does not — plus the CR 122.6 enters-with-counters
 * case and the once-per-turn gate.
 */
class EarthKingdomGeneralTest : FunSpec({

    // Instant placing TWO +1/+1 counters on a creature you control (placer = its caster).
    val counterInfusion = card("Counter Infusion") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Put two +1/+1 counters on target creature you control."
        spell {
            val target = target("target creature you control", Targets.CreatureYouControl)
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, target)
        }
    }

    // Vanilla creature that enters with two +1/+1 counters (CR 122.6 "put" case; placer = its
    // controller per CR 122.6a).
    val counterBearer = card("Counter Bearer") {
        manaCost = "{G}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        oracleText = "Counter Bearer enters with two +1/+1 counters on it."
        replacementEffect(
            EntersWithCounters(
                counterType = CounterTypeFilter.PlusOnePlusOne,
                count = 2,
                selfOnly = true
            )
        )
    }

    // Sorcery that proliferates. Proliferate resolves through a select-permanents decision and its
    // counter placement is emitted from the continuation resumer (a path that formerly carried no
    // placer) — so this pins that resumed placements are attributed to you (CR 122.5/122.6a).
    val proliferation = card("Proliferation") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell {
            effect = Effects.Proliferate()
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EarthKingdomGeneral, counterInfusion, counterBearer, proliferation))
        return driver
    }

    test("you put +1/+1 counters on a creature: may gain that much life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Earth Kingdom General")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 1)

        driver.castSpell(player, spell, targets = listOf(creature))
        driver.bothPass()   // resolve Counter Infusion; the trigger goes on the stack
        driver.bothPass()   // resolve the trigger → may yes/no decision
        driver.submitYesNo(player, true)

        driver.getLifeTotal(player) shouldBe 22   // 20 + 2 counters placed
    }

    test("an opponent putting +1/+1 counters does not trigger it (CR 122.6a placer check)") {
        val driver = createDriver()
        // Player2 is active so they can cast at sorcery speed on their own creature.
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 1)
        val opponent = driver.player2
        val you = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Earth Kingdom General")
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val spell = driver.putCardInHand(opponent, "Counter Infusion")
        driver.giveMana(opponent, Color.GREEN, 1)

        val lifeBefore = driver.getLifeTotal(you)
        driver.castSpell(opponent, spell, targets = listOf(theirCreature))
        driver.bothPass()   // resolve — Earth Kingdom General must NOT trigger

        driver.getLifeTotal(you) shouldBe lifeBefore
        // Not even offered: no pending "may gain life" decision for our player.
        driver.state.pendingDecision shouldBe null
    }

    test("a creature entering with +1/+1 counters under your control triggers it (CR 122.6)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Earth Kingdom General")
        val spell = driver.putCardInHand(player, "Counter Bearer")
        driver.giveMana(player, Color.GREEN, 1)

        driver.castSpell(player, spell)
        // Resolve Counter Bearer (enters with 2 counters) and let the reflexive "may gain life"
        // trigger land on the stack and resolve into its yes/no decision.
        var guard = 0
        while (driver.pendingDecision == null && guard < 8) {
            driver.bothPass()
            guard++
        }
        driver.submitYesNo(player, true)

        driver.getLifeTotal(player) shouldBe 22   // 20 + 2 counters it entered with
    }

    test("triggers only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Earth Kingdom General")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell1 = driver.putCardInHand(player, "Counter Infusion")
        val spell2 = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 2)

        driver.castSpell(player, spell1, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, true)   // first placement: gain 2

        // Second placement this turn: "Do this only once each turn" — no trigger, no decision.
        driver.castSpell(player, spell2, targets = listOf(creature))
        driver.bothPass()

        driver.getLifeTotal(player) shouldBe 22   // still just the first gain
    }

    test("declining does not spend the turn — a later placement still offers the life") {
        // CR 603.2h: the rider is keyed to the gain, not to the trigger. "Once you choose to gain
        // life using Earth Kingdom General's second ability, that ability won't trigger again that
        // turn" — declining isn't choosing. Under `oncePerTurn` the second placement never fired.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Earth Kingdom General")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell1 = driver.putCardInHand(player, "Counter Infusion")
        val spell2 = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 2)

        driver.castSpell(player, spell1, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, false)  // decline the first placement

        driver.castSpell(player, spell2, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, true)   // the ability triggered again — take this one

        driver.getLifeTotal(player) shouldBe 22   // 20 + the 2 counters of the second placement
    }

    test("proliferating a +1/+1 counter onto a creature triggers it (resumed-placement placer)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Earth Kingdom General")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        // Seed one +1/+1 counter directly (no ETB) so proliferate has a kind to add to and the seed
        // itself doesn't consume the once-per-turn trigger.
        driver.replaceState(driver.state.updateEntity(creature) { c ->
            val existing = c.get<CountersComponent>() ?: CountersComponent()
            c.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        })
        val spell = driver.putCardInHand(player, "Proliferation")
        driver.giveColorlessMana(player, 2)

        driver.castSpell(player, spell)
        driver.bothPass()   // resolve Proliferation → select-permanents decision

        // Choose the seeded creature; proliferate adds one more +1/+1 counter (placer = you).
        var guard = 0
        while (driver.pendingDecision == null && guard < 8) { driver.bothPass(); guard++ }
        val select = driver.pendingDecision!!
        driver.submitDecision(player, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(creature)))

        // Earth Kingdom General's "may gain life" trigger lands and resolves into its yes/no.
        guard = 0
        while (driver.pendingDecision == null && guard < 8) { driver.bothPass(); guard++ }
        driver.submitYesNo(player, true)

        driver.getLifeTotal(player) shouldBe 21   // 20 + 1 counter proliferated on
    }
})
