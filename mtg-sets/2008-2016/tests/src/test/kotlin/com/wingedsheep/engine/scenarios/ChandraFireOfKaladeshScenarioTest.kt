package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.ChandraFireOfKaladesh
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Chandra, Fire of Kaladesh // Chandra, Roaring Flame (ORI #135).
 *
 * Front: a red spell untaps her; her {T} ping flips her once she has dealt 3 damage this turn,
 * checked only as the ability resolves. Back −7: 6 damage to each opponent at once, and only the
 * players actually dealt damage get the upkeep-burn emblem — a fully prevented opponent gets none,
 * and the player a replacement redirected the damage to does (rulings 2015-06-22).
 */
class ChandraFireOfKaladeshScenarioTest : FunSpec({
    val shield = card("Test Damage Shield") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Prevent all damage that would be dealt to you."
        replacementEffect(PreventDamage(appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)))
    }
    val mirror = card("Test Damage Mirror") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "All damage that would be dealt to you is dealt to its source's controller instead."
        replacementEffect(
            RedirectDamage(
                redirectTo = EffectTarget.ControllerOfDamageSource,
                appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
            )
        )
    }

    val front = ChandraFireOfKaladesh.script
    val back = ChandraFireOfKaladesh.backFace!!.script
    val pingId = front.activatedAbilities[0].id
    val ultimateId = back.activatedAbilities[2].id

    fun GameTestDriver.drain() {
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 40) {
            if (isPaused) autoResolveDecision() else passPriority(state.priorityPlayerId!!)
        }
    }

    /** Flips a fresh Chandra by pinging her own controller three times, then sets her loyalty. */
    fun GameTestDriver.putRoaringFlame(player: EntityId, loyalty: Int): EntityId {
        val chandra = putCreatureOnBattlefield(player, "Chandra, Fire of Kaladesh")
        removeSummoningSickness(chandra)
        repeat(3) {
            untapPermanent(chandra)
            submitSuccess(ActivateAbility(player, chandra, pingId, targets = listOf(ChosenTarget.Player(player))))
            drain()
        }
        val flame = findPermanent(player, "Chandra, Roaring Flame").shouldNotBeNull()
        addComponent(flame, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return flame
    }

    fun GameTestDriver.emblemOwners(): List<EntityId> = state.globalGrantedTriggeredAbilities.map { it.controllerId }

    test("−7 burns every opponent at once, and only the opponent actually dealt damage gets the emblem") {
        val d = GameTestDriver().apply { registerCards(TestCards.all + listOf(ChandraFireOfKaladesh, shield)) }
        val (me, hit, shielded) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) }, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(shielded, "Test Damage Shield")
        val chandra = d.putRoaringFlame(me, loyalty = 7)

        d.submitSuccess(ActivateAbility(me, chandra, ultimateId))
        d.drain()

        withClue("Chandra left at 0 loyalty, yet she still dealt the damage") {
            d.state.getBattlefield().contains(chandra) shouldBe false
        }
        d.getLifeTotal(hit) shouldBe 14
        d.getLifeTotal(shielded) shouldBe 20
        withClue("her controller took only the 3 flip pings") { d.getLifeTotal(me) shouldBe 17 }
        d.emblemOwners() shouldBe listOf(hit)
        val emblem = d.state.globalGrantedTriggeredAbilities.single()
        d.state.getEntity(emblem.sourceId)!!.get<CardComponent>()!!.colors.shouldBeEmpty()

        d.passPriorityUntil(Step.UPKEEP, hit)
        d.drain()
        withClue("the emblem burns its owner on their own upkeep") { d.getLifeTotal(hit) shouldBe 11 }
        d.getLifeTotal(shielded) shouldBe 20
    }

    test("damage redirected to Chandra's controller gives that player the emblem instead") {
        val d = GameTestDriver().apply { registerCards(TestCards.all + listOf(ChandraFireOfKaladesh, mirror)) }
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Damage Mirror")
        val chandra = d.putRoaringFlame(me, loyalty = 8)

        d.submitSuccess(ActivateAbility(me, chandra, ultimateId))
        d.drain()

        d.getLifeTotal(opp) shouldBe 20
        d.getLifeTotal(me) shouldBe 20 - 3 - 6
        d.emblemOwners() shouldBe listOf(me)
    }

    test("a red spell untaps her, and the third point of damage she deals this turn flips her") {
        val d = GameTestDriver().apply { registerCards(TestCards.all + listOf(ChandraFireOfKaladesh)) }
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = d.putCreatureOnBattlefield(me, "Chandra, Fire of Kaladesh")
        d.removeSummoningSickness(chandra)
        val atOpponent = listOf(ChosenTarget.Player(opp))

        fun ping() {
            d.submitSuccess(ActivateAbility(me, chandra, pingId, targets = atOpponent))
            d.drain()
        }
        fun boltOpponent() {
            val bolt = d.putCardInHand(me, "Lightning Bolt")
            d.giveMana(me, Color.RED, 1)
            d.castSpellWithTargets(me, bolt, atOpponent).error shouldBe null
            d.drain()
        }

        ping()
        d.isTapped(chandra) shouldBe true
        boltOpponent()
        withClue("casting a red spell untapped her") { d.isTapped(chandra) shouldBe false }
        ping()
        withClue("two damage dealt by Chandra: no flip yet") {
            d.findPermanent(me, "Chandra, Fire of Kaladesh") shouldBe chandra
        }
        boltOpponent()
        ping()

        d.getLifeTotal(opp) shouldBe 20 - 1 - 3 - 1 - 3 - 1
        val flame = d.findPermanent(me, "Chandra, Roaring Flame").shouldNotBeNull()
        d.state.getEntity(flame)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
        d.state.projectedState.isCreature(flame) shouldBe false
    }
})
