package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.VodalianHexcatcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class VodalianHexcatcherScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + VodalianHexcatcher)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun activation(controller: EntityId, source: EntityId, spell: EntityId, sacrifice: EntityId) = ActivateAbility(
        playerId = controller,
        sourceId = source,
        abilityId = VodalianHexcatcher.activatedAbilities.single().id,
        targets = listOf(ChosenTarget.Spell(spell)),
        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrifice)),
    )

    for (sacrificeSelf in listOf(false, true)) {
        for (payTax in listOf(false, true)) {
            test("sacrifice self=$sacrificeSelf is paid on activation and opponent pays tax=$payTax") {
                val d = driver()
                val caster = d.player1
                val controller = d.getOpponent(caster)
                val source = d.putCreatureOnBattlefield(controller, "Vodalian Hexcatcher")
                val other = d.putCreatureOnBattlefield(controller, "Island Walker")
                val sacrifice = if (sacrificeSelf) source else other
                d.putLandOnBattlefield(caster, "Forest")
                val bolt = d.putCardInHand(caster, "Lightning Bolt")
                d.giveMana(caster, Color.RED, 1)
                d.castSpell(caster, bolt, listOf(controller)).error shouldBe null
                d.passPriority(caster)
                d.submitSuccess(activation(controller, source, bolt, sacrifice))
                d.getGraveyard(controller) shouldContain sacrifice
                d.getStackSpellNames() shouldContain "Lightning Bolt"
                d.bothPass()
                val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                decision.playerId shouldBe caster
                d.submitYesNo(caster, payTax)
                if (payTax) {
                    d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
                    d.submitManaAutoPayOrDecline(caster, autoPay = true)
                    d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
                    d.bothPass()
                    d.getLifeTotal(controller) shouldBe 17
                } else {
                    d.getStackSpellNames() shouldNotContain "Lightning Bolt"
                    d.getLifeTotal(controller) shouldBe 20
                }
                d.getGraveyard(caster) shouldContain bolt
            }
        }
    }

    test("flash allows casting on the opponent's turn") {
        val d = driver()
        val controller = d.getOpponent(d.player1)
        val hexcatcher = d.putCardInHand(controller, "Vodalian Hexcatcher")
        d.passPriority(d.player1)
        d.giveMana(controller, Color.BLUE, 2)
        d.castSpell(controller, hexcatcher).error shouldBe null
        d.bothPass()
        d.findPermanent(controller, "Vodalian Hexcatcher") shouldBe hexcatcher
        d.state.activePlayerId shouldBe d.player1
    }

    test("creature spells are not legal targets and an invalid activation pays no sacrifice") {
        val d = driver()
        val caster = d.player1
        val controller = d.getOpponent(caster)
        val source = d.putCreatureOnBattlefield(controller, "Vodalian Hexcatcher")
        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.passPriority(caster)
        d.submit(activation(controller, source, bears, source)).error.shouldNotBeNull()
        d.getGraveyard(controller) shouldNotContain source
        d.findPermanent(controller, "Vodalian Hexcatcher") shouldBe source
    }

    test("lord boosts only other Merfolk controlled by its controller") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Vodalian Hexcatcher")
        val own = d.putCreatureOnBattlefield(d.player1, "Island Walker")
        val opposing = d.putCreatureOnBattlefield(d.getOpponent(d.player1), "Island Walker")
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.state.projectedState.getPower(source) shouldBe 1
        d.state.projectedState.getToughness(source) shouldBe 1
        d.state.projectedState.getPower(own) shouldBe 3
        d.state.projectedState.getToughness(own) shouldBe 3
        d.state.projectedState.getPower(opposing) shouldBe 2
        d.state.projectedState.getToughness(opposing) shouldBe 2
        d.state.projectedState.getPower(bear) shouldBe 2
        d.state.projectedState.getToughness(bear) shouldBe 2
    }
})
