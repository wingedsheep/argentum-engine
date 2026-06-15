package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.SauronTheDarkLord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Sauron, the Dark Lord (LTR #224).
 *
 *   Ward—Sacrifice a legendary artifact or legendary creature.
 *   Whenever an opponent casts a spell, amass Orcs 1.
 *   Whenever an Army you control deals combat damage to a player, the Ring tempts you.
 *   Whenever the Ring tempts you, you may discard your hand. If you do, draw four cards.
 *
 * All four pieces compose existing primitives — the test proves the three triggered abilities
 * (opponent-cast amass, Army-damage Ring-tempt, Ring-tempt discard/draw) and that the
 * Ward—Sacrifice keyword is present with the right cost shape.
 */
class SauronTheDarkLordScenarioTest : FunSpec({

    val projector = StateProjector()

    // {R} instant the opponent can cast to fire Sauron's "opponent casts a spell" trigger.
    val bolt = card("Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    // A vanilla 2/2 Army to attack with (mirrors AmassScenarioTest's Zombie Army shape).
    val orcArmy = card("Test Orc Army") {
        manaCost = "{0}"
        typeLine = "Creature — Orc Army"
        power = 2; toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SauronTheDarkLord, bolt, orcArmy))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    fun GameTestDriver.armiesControlledBy(player: EntityId): List<EntityId> {
        val projected = projector.project(state)
        return projected.getBattlefieldControlledBy(player)
            .filter { projected.isCreature(it) && projected.hasSubtype(it, "Army") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.temptCount(player: EntityId): Int =
        state.getEntity(player)?.get<TheRingComponent>()?.temptCount ?: 0

    test("Ward—Sacrifice a legendary artifact or legendary creature keyword is present") {
        val ward = SauronTheDarkLord.keywordAbilities
            .filterIsInstance<KeywordAbility.Ward>()
            .single()
        (ward.cost is WardCost.Sacrifice) shouldBe true
    }

    test("opponent casting a spell amasses Orcs 1 (an Army appears and grows)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Sauron, the Dark Lord")
        driver.armiesControlledBy(controller).size shouldBe 0

        // Opponent casts a spell — Sauron's controller amasses Orcs 1. The non-active opponent only
        // has priority after the active player passes, so hand priority over before casting.
        driver.giveMana(opponent, Color.RED, 1)
        val boltCard = driver.putCardInHand(opponent, "Test Bolt")
        driver.passPriority(controller)
        driver.castSpell(opponent, boltCard)
        driver.resolveStack()

        val army = driver.armiesControlledBy(controller).single()
        projector.project(driver.state).hasSubtype(army, "Orc") shouldBe true
        driver.plusOneCounters(army) shouldBe 1

        // A second opponent spell grows the same Army to 2 counters.
        driver.giveMana(opponent, Color.RED, 1)
        val boltCard2 = driver.putCardInHand(opponent, "Test Bolt")
        driver.passPriority(controller)
        driver.castSpell(opponent, boltCard2)
        driver.resolveStack()

        driver.armiesControlledBy(controller) shouldBe listOf(army)
        driver.plusOneCounters(army) shouldBe 2
    }

    test("an Army you control dealing combat damage to a player tempts you, then you may discard to draw four") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val defender = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Sauron, the Dark Lord")
        val army = driver.putCreatureOnBattlefield(controller, "Test Orc Army")
        driver.removeSummoningSickness(army)
        driver.temptCount(controller) shouldBe 0

        // Give the controller a couple of cards to discard so the draw is observable.
        repeat(2) { driver.putCardInHand(controller, "Mountain") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(controller, listOf(army), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(defender)

        // Combat damage is dealt: the Army hits the defending player → the Ring tempts the controller.
        // The Ring-tempt trigger then offers "you may discard your hand; if you do, draw four".
        var guard = 0
        while (driver.state.pendingDecision == null && guard < 30) { driver.bothPass(); guard++ }

        driver.temptCount(controller) shouldNotBe 0
        driver.temptCount(controller) shouldBe 1

        // First pending decision is the Ring-bearer choice (resolve it), then the discard yes/no.
        driver.autoResolveDecision()
        guard = 0
        while (driver.state.pendingDecision == null && guard < 30) { driver.bothPass(); guard++ }

        driver.submitYesNo(controller, true)
        driver.resolveStack()

        // The hand (2 lands) was discarded and exactly four cards were drawn.
        driver.getHand(controller).size shouldBe 4
    }
})
