package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.NissaVastwoodSeer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Nissa, Vastwood Seer // Nissa, Sage Animist (ORI #189).
 *
 * Front: a land entering under your control flips her only if you then control seven or more
 * lands (intervening if). Back +1: the revealed top card goes onto the battlefield if it's a land,
 * otherwise into your hand. −2: the legendary 4/4 Ashaya token. −7: up to six target lands — any
 * player's — untap and become 6/6 Elemental creature lands for good (no "until end of turn").
 */
class NissaVastwoodSeerScenarioTest : FunSpec({

    val back = NissaVastwoodSeer.backFace!!.script
    val plusOneId = back.activatedAbilities[0].id
    val minusTwoId = back.activatedAbilities[1].id
    val minusSevenId = back.activatedAbilities[2].id

    /** Test-only: puts every land card in your hand onto the battlefield at once. */
    val twinPlanting = card("Twin Planting") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val lands = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Land))
                move(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
            }
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NissaVastwoodSeer + twinPlanting)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 40) {
            if (isPaused) autoResolveDecision() else passPriority(state.priorityPlayerId!!)
        }
    }

    /** Nissa plus [landsInPlay] Forests on the battlefield, then a Forest played from hand. */
    fun GameTestDriver.playLandBeside(landsInPlay: Int): EntityId {
        val me = activePlayer!!
        val nissa = putCreatureOnBattlefield(me, "Nissa, Vastwood Seer")
        repeat(landsInPlay) { putLandOnBattlefield(me, "Forest") }
        playLand(me, findCardInHand(me, "Forest")!!).error shouldBe null
        drain()
        return nissa
    }

    fun GameTestDriver.sageAnimist(): EntityId {
        playLandBeside(6)
        return findPermanent(activePlayer!!, "Nissa, Sage Animist").shouldNotBeNull()
    }

    fun GameTestDriver.loyalty(id: EntityId): Int =
        state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    test("the seventh land entering flips her into Nissa, Sage Animist with 3 loyalty") {
        val d = driver()
        d.playLandBeside(6)

        d.findPermanent(d.activePlayer!!, "Nissa, Vastwood Seer") shouldBe null
        val animist = d.findPermanent(d.activePlayer!!, "Nissa, Sage Animist").shouldNotBeNull()
        d.loyalty(animist) shouldBe 3
        d.state.projectedState.isCreature(animist) shouldBe false
    }

    test("a land entering that leaves you with only six lands doesn't flip her") {
        val d = driver()
        val nissa = d.playLandBeside(5)

        d.findPermanent(d.activePlayer!!, "Nissa, Vastwood Seer") shouldBe nissa
        d.findPermanent(d.activePlayer!!, "Nissa, Sage Animist") shouldBe null
    }

    test("+1 puts a revealed land onto the battlefield and a revealed nonland into hand") {
        val d = driver()
        val me = d.activePlayer!!
        val animist = d.sageAnimist()
        val forest = d.putCardOnTopOfLibrary(me, "Forest")

        d.submitSuccess(ActivateAbility(me, animist, plusOneId))
        d.drain()
        d.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain forest
        d.loyalty(animist) shouldBe 4

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        val bears = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        d.submitSuccess(ActivateAbility(me, animist, plusOneId))
        d.drain()
        d.getHand(me) shouldContain bears
        d.loyalty(animist) shouldBe 5
    }

    test("−2 creates Ashaya, the Awoken World, a legendary 4/4 green Elemental token") {
        val d = driver()
        val me = d.activePlayer!!
        val animist = d.sageAnimist()

        d.submitSuccess(ActivateAbility(me, animist, minusTwoId))
        d.drain()

        val ashaya = d.findPermanent(me, "Ashaya, the Awoken World").shouldNotBeNull()
        val projected = d.state.projectedState
        projected.isCreature(ashaya) shouldBe true
        projected.isLegendary(ashaya) shouldBe true
        projected.getPower(ashaya) shouldBe 4
        projected.getToughness(ashaya) shouldBe 4
        projected.hasColor(ashaya, Color.GREEN) shouldBe true
        projected.hasSubtype(ashaya, "Elemental") shouldBe true
        d.loyalty(animist) shouldBe 1
    }

    test("−7 untaps up to six target lands of any player and they stay 6/6 Elemental lands") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val animist = d.sageAnimist()
        d.addComponent(animist, CountersComponent(mapOf(CounterType.LOYALTY to 7)))

        val mine = d.getLands(me).take(2).onEach { d.tapPermanent(it) }
        val theirs = d.putLandOnBattlefield(opp, "Forest").also { d.tapPermanent(it) }
        val untouched = d.getLands(me).first { it !in mine }
        val targets = mine + theirs

        d.submitSuccess(
            ActivateAbility(me, animist, minusSevenId, targets = targets.map { ChosenTarget.Permanent(it) })
        )
        d.drain()

        withClue("Nissa paid all 7 loyalty and is gone") {
            d.findPermanent(me, "Nissa, Sage Animist") shouldBe null
        }
        fun assertAnimated() {
            val projected = d.state.projectedState
            for (land in targets) {
                projected.isCreature(land) shouldBe true
                projected.hasType(land, "LAND") shouldBe true
                projected.hasSubtype(land, "Forest") shouldBe true
                projected.hasSubtype(land, "Elemental") shouldBe true
                projected.getPower(land) shouldBe 6
                projected.getToughness(land) shouldBe 6
            }
            projected.isCreature(untouched) shouldBe false
        }
        targets.forEach { d.isTapped(it) shouldBe false }
        assertAnimated()

        d.passPriorityUntil(Step.UPKEEP, opp)
        withClue("no \"until end of turn\": the lands are still 6/6 creatures on the next turn") {
            assertAnimated()
        }
    }

    test("two lands entering at once with six out flip her once, into a 3-loyalty Nissa, Sage Animist") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Nissa, Vastwood Seer")
        repeat(6) { d.putLandOnBattlefield(me, "Forest") }
        var s = d.state
        for (id in s.getZone(ZoneKey(me, Zone.HAND))) {
            s = s.removeFromZone(ZoneKey(me, Zone.HAND), id).addToZone(ZoneKey(me, Zone.EXILE), id)
        }
        d.replaceState(s)
        val pair = List(2) { d.putCardInHand(me, "Forest") }
        val planting = d.putCardInHand(me, "Twin Planting")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, planting).error shouldBe null
        d.drain()

        withClue("both Forests entered together, leaving eight lands") {
            pair.forEach { d.state.getBattlefield() shouldContain it }
            d.getLands(me).size shouldBe 8
        }
        withClue("the first trigger flipped her; the second found its source a new object and did nothing") {
            d.state.stack.shouldBeEmpty()
            d.findPermanent(me, "Nissa, Vastwood Seer") shouldBe null
            d.state.getBattlefield().count {
                d.state.getEntity(it)?.get<CardComponent>()?.name == "Nissa, Sage Animist"
            } shouldBe 1
        }
        val animist = d.findPermanent(me, "Nissa, Sage Animist").shouldNotBeNull()
        d.loyalty(animist) shouldBe 3
        d.state.projectedState.isCreature(animist) shouldBe false
    }
})
