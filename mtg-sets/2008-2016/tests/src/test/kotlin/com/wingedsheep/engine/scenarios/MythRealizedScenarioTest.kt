package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.MythRealized
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Myth Realized (DTK #26) — lore counters on noncreature casts and on {2}{W}; {W} animates it into
 * a Monk Avatar whose power and toughness are each equal to the lore counters on it.
 *
 * The thing worth proving is that the granted P/T is a live characteristic-defining value, not a
 * size frozen when the animation resolves: a lore counter added afterwards — the usual play, cast a
 * spell with the Avatar already attacking — must grow it. It also has to stay an enchantment, and
 * only noncreature spells feed it.
 */
class MythRealizedScenarioTest : FunSpec({

    val addLoreAbility = MythRealized.activatedAbilities[0].id
    val animateAbility = MythRealized.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun GameTestDriver.lore(myth: EntityId): Int =
        state.getEntity(myth)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun GameTestDriver.addLore(player: EntityId, myth: EntityId) {
        giveColorlessMana(player, 2)
        giveMana(player, Color.WHITE, 1)
        submit(ActivateAbility(playerId = player, sourceId = myth, abilityId = addLoreAbility))
            .outcome shouldBe Outcome.Done
        resolveStack()
    }

    fun GameTestDriver.animate(player: EntityId, myth: EntityId) {
        giveMana(player, Color.WHITE, 1)
        submit(ActivateAbility(playerId = player, sourceId = myth, abilityId = animateAbility))
            .outcome shouldBe Outcome.Done
        resolveStack()
    }

    test("noncreature spells add a lore counter; creature spells do not") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val myth = d.putPermanentOnBattlefield(me, "Myth Realized")

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).outcome shouldBe Outcome.Done
        d.resolveStack()
        d.lore(myth) shouldBe 1

        val bears = d.putCardInHand(me, "Grizzly Bears")
        d.giveColorlessMana(me, 1)
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, bears).outcome shouldBe Outcome.Done
        d.resolveStack()
        withClue("a creature spell is not a noncreature spell") {
            d.lore(myth) shouldBe 1
        }

        d.addLore(me, myth)
        d.lore(myth) shouldBe 2
    }

    test("animated, it is a Monk Avatar enchantment creature whose size tracks its lore counters live") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val myth = d.putPermanentOnBattlefield(me, "Myth Realized")
        repeat(2) { d.addLore(me, myth) }

        d.animate(me, myth)

        val projected = d.state.projectedState
        projected.isCreature(myth) shouldBe true
        projected.getPower(myth) shouldBe 2
        projected.getToughness(myth) shouldBe 2
        withClue("\"in addition to its other types\" — still an enchantment, still white") {
            projected.hasType(myth, "ENCHANTMENT") shouldBe true
            projected.getColors(myth) shouldBe setOf(Color.WHITE.name)
        }
        projected.hasSubtype(myth, "Monk") shouldBe true
        projected.hasSubtype(myth, "Avatar") shouldBe true

        // Counters added after the animation resolved still count — from the cast trigger ...
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).outcome shouldBe Outcome.Done
        d.resolveStack()
        d.state.projectedState.getPower(myth) shouldBe 3
        d.state.projectedState.getToughness(myth) shouldBe 3

        // ... and from its own {2}{W} ability.
        d.addLore(me, myth)
        d.state.projectedState.getPower(myth) shouldBe 4
        d.state.projectedState.getToughness(myth) shouldBe 4
    }

    test("the animation ends with the turn; the lore counters stay") {
        val d = driver()
        val me = d.activePlayer!!
        val myth = d.putPermanentOnBattlefield(me, "Myth Realized")
        d.addLore(me, myth)

        d.animate(me, myth)
        d.state.projectedState.isCreature(myth) shouldBe true

        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.isCreature(myth) shouldBe false
        d.state.projectedState.hasType(myth, "ENCHANTMENT") shouldBe true
        d.lore(myth) shouldBe 1
    }
})
