package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.DanceOfTheManse
import com.wingedsheep.mtg.sets.definitions.exo.cards.SoulWarden
import com.wingedsheep.mtg.sets.definitions.lea.cards.SolRing
import com.wingedsheep.mtg.sets.definitions.mir.cards.Pacifism
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.mtg.sets.definitions.vis.cards.PhyrexianWalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dance of the Manse — "Return up to X target artifact and/or non-Aura enchantment cards each with
 * mana value X or less from your graveyard to the battlefield. If X is 6 or more, those permanents
 * are 4/4 creatures in addition to their other types."
 *
 * Pins the ruling that with X ≥ 6 they *enter* as 4/4 creatures (a "creature enters" trigger sees
 * each one, and an artifact creature becomes 4/4), that with X < 6 they return unchanged, and the
 * targeting limits: mana value X or less, and no Auras.
 */
class DanceOfTheManseScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(DanceOfTheManse, SoulWarden, SolRing, Pacifism, GloriousAnthem, PhyrexianWalker))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castDance(me: EntityId, x: Int, targets: List<EntityId>) = run {
        val dance = putCardInHand(me, "Dance of the Manse")
        giveMana(me, Color.WHITE, 1)
        giveMana(me, Color.BLUE, 1 + x)
        submit(
            CastSpell(
                playerId = me,
                cardId = dance,
                targets = targets.map { ChosenTarget.Card(it, me, Zone.GRAVEYARD) },
                xValue = x,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("X of 6 or more: they enter as 4/4 creatures in addition to their other types") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Soul Warden")
        val ring = d.putCardInGraveyard(me, "Sol Ring")
        val anthem = d.putCardInGraveyard(me, "Glorious Anthem")
        val walker = d.putCardInGraveyard(me, "Phyrexian Walker")
        val life = d.getLifeTotal(me)

        d.castDance(me, 6, listOf(ring, anthem, walker)).error shouldBe null
        d.bothPass()
        repeat(4) { if (d.state.stack.isNotEmpty()) d.bothPass() }

        val projected = d.state.projectedState
        listOf(ring, anthem, walker).forEach {
            (it in d.state.getBattlefield()) shouldBe true
            projected.isCreature(it) shouldBe true
        }
        projected.getTypes(ring) shouldContain "ARTIFACT"
        projected.getTypes(anthem) shouldContain "ENCHANTMENT"
        // 4/4, plus Glorious Anthem's +1/+1 now that each is a creature you control — the 0/3
        // Phyrexian Walker included: it's a 4/4 creature, not a 0/3 one.
        projected.getPower(ring) shouldBe 5
        projected.getToughness(walker) shouldBe 5
        projected.getPower(walker) shouldBe 5
        // Soul Warden saw three creatures enter.
        d.getLifeTotal(me) shouldBe life + 3
    }

    test("X below 6: they return as they are") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Soul Warden")
        val ring = d.putCardInGraveyard(me, "Sol Ring")
        val anthem = d.putCardInGraveyard(me, "Glorious Anthem")
        val life = d.getLifeTotal(me)

        d.castDance(me, 3, listOf(ring, anthem)).error shouldBe null
        d.bothPass()

        (ring in d.state.getBattlefield()) shouldBe true
        (anthem in d.state.getBattlefield()) shouldBe true
        d.state.projectedState.isCreature(ring) shouldBe false
        d.state.projectedState.isCreature(anthem) shouldBe false
        d.getLifeTotal(me) shouldBe life
    }

    test("targets are capped at mana value X, and an Aura is never a legal target") {
        val d = driver()
        val me = d.activePlayer!!
        val anthem = d.putCardInGraveyard(me, "Glorious Anthem")
        val pacifism = d.putCardInGraveyard(me, "Pacifism")

        d.castDance(me, 2, listOf(anthem)).error.shouldNotBeNull()
        d.castDance(me, 6, listOf(pacifism)).error.shouldNotBeNull()
    }
})
