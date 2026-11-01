package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Riot granted to a creature *spell* (CR 702.136): a keyword frozen onto the spell's
 * `entryKeywordGrants` — here by a mana rider, "if that mana is spent on a creature spell, it gains
 * riot" (Domri, Chaos Bringer) — must give the real enters-with counter-or-haste choice, not a
 * display-only keyword. Each grant is its own instance alongside any lord's (CR 702.136b).
 */
class SpellGrantedRiotTest : FunSpec({

    val riotSpring = card("Test Riot Spring") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{T}: Add {G}. If that mana is spent on a creature spell, it gains riot."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(
                Color.GREEN,
                riders = setOf(
                    ManaSpellRider.GrantsKeywordWhenSpent(Keyword.RIOT, GameObjectFilter.Creature, Duration.Permanent)
                )
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }
    val springAbility = riotSpring.activatedAbilities[0].id

    val rioter = card("Test Spring Rioter") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        riot()
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(riotSpring, rioter))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.chooseRiot(player: EntityId, haste: Boolean) {
        val pick = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe player
        submitDecision(player, OptionChosenResponse(pick.id, if (haste) 1 else 0)).error shouldBe null
    }

    /** Casts Grizzly Bears with one rider mana (or none) plus ordinary green, and lets it resolve. */
    fun GameTestDriver.castBears(me: EntityId, withRider: Boolean, name: String = "Grizzly Bears"): EntityId {
        val bears = putCardInHand(me, name)
        if (withRider) {
            val spring = putPermanentOnBattlefield(me, "Test Riot Spring")
            submitSuccess(ActivateAbility(playerId = me, sourceId = spring, abilityId = springAbility))
            giveMana(me, Color.GREEN, 1)
        } else {
            giveMana(me, Color.GREEN, 2)
        }
        castSpell(me, bears).error shouldBe null
        bothPass()
        return bears
    }

    test("a creature spell paid with the rider mana enters with your choice of a +1/+1 counter") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.castBears(me, withRider = true)

        (bears in d.state.getBattlefield()) shouldBe false // the choice comes before it enters
        d.chooseRiot(me, haste = false)

        (bears in d.state.getBattlefield()) shouldBe true
        d.plusOneCounters(bears) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
    }

    test("choosing haste gives it haste and no counter") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.castBears(me, withRider = true)
        d.chooseRiot(me, haste = true)

        d.plusOneCounters(bears) shouldBe 0
        d.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }

    test("the spell's riot and a lord's riot are separate instances: two questions") {
        val d = driver()
        val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")
        val bears = d.castBears(me, withRider = true)
        d.chooseRiot(me, haste = false)
        d.chooseRiot(me, haste = false)

        d.plusOneCounters(bears) shouldBe 2
    }

    test("a creature with printed riot asks twice: its own riot, then the spell's granted one") {
        val d = driver()
        val me = d.activePlayer!!
        val goblin = d.castBears(me, withRider = true, name = "Test Spring Rioter")
        d.chooseRiot(me, haste = false)
        d.chooseRiot(me, haste = true)

        (d.pendingDecision == null) shouldBe true
        d.plusOneCounters(goblin) shouldBe 1
        d.state.projectedState.hasKeyword(goblin, Keyword.HASTE) shouldBe true
    }

    test("cast with ordinary mana it gets no riot choice") {
        val d = driver()
        val me = d.activePlayer!!
        val bears = d.castBears(me, withRider = false)

        (d.pendingDecision == null) shouldBe true
        (bears in d.state.getBattlefield()) shouldBe true
        d.plusOneCounters(bears) shouldBe 0
    }
})
