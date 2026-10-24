package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.CodieVociferousCodex
import com.wingedsheep.mtg.sets.definitions.woe.cards.PicklockPrankster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Codie, Vociferous Codex (STX #253).
 *
 * The drawback is judged against the face being cast: a creature spell is banned, an instant is
 * not, and an Adventure card's instant half stays castable while its creature half doesn't.
 *
 * The mana ability's rider waits for the next spell, walks the library past a card of *equal*
 * mana value (not lesser) and a creature to the first cheaper instant, leaves that instant in exile
 * castable for free, and bottoms only the misses. Casting the free instant is a second spell this
 * turn and must not fire the one-shot rider again.
 */
class CodieVociferousCodexScenarioTest : FunSpec({

    val bigSorcery = card("Codie Test Sorcery") {
        manaCost = "{3}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    val cheapInstant = card("Codie Test Instant") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(5) }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CodieVociferousCodex, PicklockPrankster, bigSorcery, cheapInstant))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.codie(player: EntityId): EntityId =
        putPermanentOnBattlefield(player, "Codie, Vociferous Codex").also { removeSummoningSickness(it) }

    fun GameTestDriver.offers(player: EntityId, card: EntityId): List<CastSpell> =
        legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == card }

    fun GameTestDriver.poolTotal(player: EntityId): Int =
        state.getEntity(player)!!.get<ManaPoolComponent>()!!.let {
            it.white + it.blue + it.black + it.red + it.green + it.colorless
        }

    test("you can't cast creature spells, but instants are still castable") {
        val d = setup()
        val me = d.activePlayer!!
        d.codie(me)
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.GREEN, 2)
        d.giveMana(me, Color.RED, 1)

        d.offers(me, bears).shouldBeEmpty()
        d.submitExpectFailure(CastSpell(me, bears, paymentStrategy = PaymentStrategy.FromPool))
        d.offers(me, bolt).isNotEmpty() shouldBe true
    }

    test("an Adventure's instant half is castable while its creature half is not") {
        val d = setup()
        val me = d.activePlayer!!
        d.codie(me)
        val prankster = d.putCardInHand(me, "Picklock Prankster")
        d.giveMana(me, Color.BLUE, 2)

        val offers = d.offers(me, prankster)
        offers.any { it.faceIndex == null } shouldBe false
        d.submitExpectFailure(CastSpell(me, prankster, paymentStrategy = PaymentStrategy.FromPool))
        val adventure = offers.first { it.faceIndex == 0 }
        d.submitSuccess(adventure)
        d.stackSize shouldBe 1
    }

    test("the next spell exiles to a cheaper instant, castable for free; only the misses go to the bottom") {
        val d = setup()
        val me = d.activePlayer!!
        val codie = d.codie(me)

        // Library top, in order: an equal-MV sorcery (not lesser), a creature, then the hit.
        val hit = d.putCardOnTopOfLibrary(me, "Codie Test Instant")
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val equal = d.putCardOnTopOfLibrary(me, "Codie Test Sorcery")
        val librarySize = d.state.getLibrary(me).size

        d.giveColorlessMana(me, 4)
        val abilityId = d.cardRegistry.requireCard("Codie, Vociferous Codex").activatedAbilities[0].id
        d.submitSuccess(ActivateAbility(me, codie, abilityId, paymentStrategy = PaymentStrategy.FromPool))
        d.poolTotal(me) shouldBe 5

        val trigger = d.putCardInHand(me, "Codie Test Sorcery")
        d.submitSuccess(CastSpell(me, trigger, paymentStrategy = PaymentStrategy.FromPool))
        val lifeBefore = d.getLifeTotal(me)

        // The delayed trigger resolves first, above the sorcery that caused it.
        d.bothPass()
        d.getExile(me) shouldContainExactly listOf(hit)
        d.state.getLibrary(me).size shouldBe librarySize - 1
        d.state.getLibrary(me).takeLast(2) shouldContainExactlyInAnyOrder listOf(bears, equal)

        // The hit is cast for free; the two leftover mana stay in the pool.
        val pool = d.poolTotal(me)
        // The exile permission's free-cast rider prices the ordinary offer at {0}.
        val free = d.offers(me, hit).first()
        d.submitSuccess(free.copy(paymentStrategy = PaymentStrategy.FromPool))
        d.poolTotal(me) shouldBe pool

        // Resolve the instant, then the triggering sorcery. The free instant was a second spell
        // this turn: the one-shot rider is spent, so nothing else is exiled.
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(me) shouldBe lifeBefore + 5 + 1
        d.getExile(me).shouldBeEmpty()
        d.state.getLibrary(me).size shouldBe librarySize - 1
    }
})
