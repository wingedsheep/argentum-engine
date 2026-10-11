package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantOffspringToOwnSpells
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Offspring granted to creature spells as they are cast (CR 702.175, `GrantOffspringToOwnSpells` —
 * Zinnia, Valley's Voice). CR 702.175a: offspring is "you may pay an additional [cost] as you cast
 * this spell" plus "when this permanent enters, if its offspring cost was paid, create a token that's
 * a copy of it, except it's 1/1". CR 702.175b: each instance is paid separately and triggers off its
 * own payment only — so the granted cost rides its own slot, never the kicker one.
 */
class GrantedOffspringTest : FunSpec({

    val granter = card("Test Offspring Granter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = GrantOffspringToOwnSpells(cost = ManaCost.parse("{2}")) }
    }

    /** "As long as you have 25 or more life, creature spells you cast gain offspring {2}." */
    val gatedGranter = card("Test Gated Offspring Granter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantOffspringToOwnSpells(cost = ManaCost.parse("{2}"))
            condition = Conditions.LifeAtLeast(25)
        }
    }

    /** "Spells you cast gain offspring {2}" — a filter wider than any printed card's. */
    val broadGranter = card("Test Broad Offspring Granter") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = GrantOffspringToOwnSpells(cost = ManaCost.parse("{2}"), spellFilter = GameObjectFilter.Any)
        }
    }

    val bear = card("Test Offspring Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }

    /** Printed kicker whose payoff must not fire when only the granted offspring was paid. */
    val kickerBear = card("Test Kicker Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.kicker("{3}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.WasKicked
            effect = Effects.GainLife(5)
        }
    }

    val shock = card("Test Offspring Sorcery") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    fun driver(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(granter, gatedGranter, broadGranter, bear, kickerBear, shock))
        d.initMirrorMatch(Deck.of("Forest" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to listOf(d.activePlayer!!, d.getOpponent(d.activePlayer!!))
    }

    fun GameTestDriver.offspringCasts(player: EntityId, cardId: EntityId) =
        legalActions(player).mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId && it.declaredCostSlot == ChoiceSlot.OFFSPRING }

    fun GameTestDriver.castFromPool(player: EntityId, cardId: EntityId, slot: ChoiceSlot?) =
        submit(CastSpell(player, cardId, paymentStrategy = PaymentStrategy.FromPool, declaredCostSlot = slot))

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) {
            pendingDecision?.let { autoResolveDecision() } ?: bothPass()
        }
    }

    fun GameTestDriver.creaturesNamed(player: EntityId, name: String) =
        getCreatures(player).filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("a creature spell you cast gains an Offspring cast variant; paying it makes a 1/1 token copy") {
        val (d, players) = driver()
        val (p1, _) = players
        d.putPermanentOnBattlefield(p1, "Test Offspring Granter")
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")

        d.offspringCasts(p1, bearCard) shouldHaveSize 1

        d.giveMana(p1, Color.GREEN, 4)
        d.castFromPool(p1, bearCard, ChoiceSlot.OFFSPRING).error shouldBe null
        withClue("the granted {2} is charged on top of {1}{G}") { d.state.getEntity(p1)?.get<ManaPoolComponent>()?.total shouldBe 0 }
        d.resolveStack()

        val bears = d.creaturesNamed(p1, "Test Offspring Bear")
        bears shouldHaveSize 2
        val token = bears.single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.state.projectedState.getPower(token) shouldBe 1
        d.state.projectedState.getToughness(token) shouldBe 1
        withClue("the token copy isn't cast and paid nothing — it doesn't trigger again") {
            d.stackSize shouldBe 0
        }
    }

    test("declining the granted cost makes no token") {
        val (d, players) = driver()
        val (p1, _) = players
        d.putPermanentOnBattlefield(p1, "Test Offspring Granter")
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")

        d.giveMana(p1, Color.GREEN, 2)
        d.castFromPool(p1, bearCard, null).error shouldBe null
        d.resolveStack()

        d.creaturesNamed(p1, "Test Offspring Bear") shouldHaveSize 1
    }

    test("the trigger keys off the payment: the token still comes if the granter left before resolution") {
        val (d, players) = driver()
        val (p1, _) = players
        val granterId = d.putPermanentOnBattlefield(p1, "Test Offspring Granter")
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")

        d.giveMana(p1, Color.GREEN, 4)
        d.castFromPool(p1, bearCard, ChoiceSlot.OFFSPRING).error shouldBe null
        d.moveToGraveyard(granterId)
        d.resolveStack()

        d.creaturesNamed(p1, "Test Offspring Bear") shouldHaveSize 2
    }

    test("no granter, a noncreature spell, or an opponent's spell: no offspring, and a forged declaration is rejected") {
        val (d, players) = driver()
        val (p1, p2) = players
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")
        d.offspringCasts(p1, bearCard).shouldBeEmpty()
        d.giveMana(p1, Color.GREEN, 4)
        d.castFromPool(p1, bearCard, ChoiceSlot.OFFSPRING).error shouldNotBe null

        d.putPermanentOnBattlefield(p1, "Test Offspring Granter")
        val sorcery = d.putCardInHand(p1, "Test Offspring Sorcery")
        withClue("the grant's filter is creature spells") { d.offspringCasts(p1, sorcery).shouldBeEmpty() }

        withClue("\"you cast\" — the granter's controller only") {
            val opposingBear = d.putCardInHand(p2, "Test Offspring Bear")
            d.giveMana(p2, Color.GREEN, 4)
            d.castFromPool(p2, opposingBear, ChoiceSlot.OFFSPRING).error shouldNotBe null
        }
    }

    test("CR 702.175b: paying the granted offspring doesn't kick a creature with printed kicker") {
        val (d, players) = driver()
        val (p1, _) = players
        d.putPermanentOnBattlefield(p1, "Test Offspring Granter")
        val kicker = d.putCardInHand(p1, "Test Kicker Bear")

        val variants = d.legalActions(p1).mapNotNull { it.action as? CastSpell }.filter { it.cardId == kicker }
        variants.map { it.declaredCostSlot }.toSet() shouldBe setOf(null, ChoiceSlot.KICKED, ChoiceSlot.OFFSPRING)

        d.giveMana(p1, Color.GREEN, 4)
        d.castFromPool(p1, kicker, ChoiceSlot.OFFSPRING).error shouldBe null
        d.resolveStack()

        d.creaturesNamed(p1, "Test Kicker Bear") shouldHaveSize 2
        withClue("the kicker payoff stays silent") { d.getLifeTotal(p1) shouldBe 20 }
    }

    test("a condition-gated grant applies only while its condition holds") {
        val (d, players) = driver()
        val (p1, _) = players
        d.putPermanentOnBattlefield(p1, "Test Gated Offspring Granter")
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")

        withClue("20 life: the gate is shut") { d.offspringCasts(p1, bearCard).shouldBeEmpty() }
        d.setLifeTotal(p1, 25)
        withClue("25 life: the gate is open") { d.offspringCasts(p1, bearCard) shouldHaveSize 1 }
    }

    test("offspring only reaches permanent spells, even under a grant whose filter matches any spell") {
        val (d, players) = driver()
        val (p1, _) = players
        d.putPermanentOnBattlefield(p1, "Test Broad Offspring Granter")
        val sorcery = d.putCardInHand(p1, "Test Offspring Sorcery")
        val bearCard = d.putCardInHand(p1, "Test Offspring Bear")

        withClue("a sorcery can't gain offspring — its declared slot would pick kicker-only effects") {
            d.offspringCasts(p1, sorcery).shouldBeEmpty()
        }
        d.offspringCasts(p1, bearCard) shouldHaveSize 1
    }
})
