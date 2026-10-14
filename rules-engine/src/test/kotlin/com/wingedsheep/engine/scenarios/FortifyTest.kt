package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fortify (CR 702.67a): "Fortify [cost]" means "[Cost]: Attach this Fortification to target land you
 * control. Activate only as a sorcery." A Fortification can be attached only to a land (CR 301.6),
 * and one attached to an illegal permanent becomes unattached but stays on the battlefield (704.5n).
 */
class FortifyTest : FunSpec({

    val testFortification = card("Test Bastion") {
        manaCost = "{2}"
        typeLine = "Artifact — Fortification"
        fortifyAbility("{3}")
    }
    val fortifyId = testFortification.activatedAbilities.single().id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + testFortification)
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.fortify(player: EntityId, fortification: EntityId, target: EntityId) =
        submit(
            ActivateAbility(
                playerId = player, sourceId = fortification, abilityId = fortifyId,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )

    fun GameTestDriver.hostOf(attachment: EntityId): EntityId? =
        state.getEntity(attachment)?.get<AttachedToComponent>()?.targetId

    test("the fortify ability is a sorcery-speed keyword line, not an equip ability") {
        val ability = testFortification.activatedAbilities.single()
        ability.isFortifyAbility shouldBe true
        ability.isEquipAbility shouldBe false
        ability.description shouldBe "Fortify {3}"
    }

    test("fortify targets only lands you control and attaches to the chosen land") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val fortification = d.putPermanentOnBattlefield(you, "Test Bastion")
        val yourLand = d.putLandOnBattlefield(you, "Forest")
        d.putLandOnBattlefield(opponent, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.giveColorlessMana(you, 3)

        val action = d.legalActions(you).single { (it.action as? ActivateAbility)?.abilityId == fortifyId }
        action.description shouldBe "Fortify {3}"
        action.validTargets!!.shouldContainExactlyInAnyOrder(d.getLands(you))

        d.fortify(you, fortification, bear).error shouldNotBe null
        d.fortify(you, fortification, yourLand).error shouldBe null
        d.bothPass()

        d.hostOf(fortification) shouldBe yourLand
    }

    test("fortify can't be activated at instant speed") {
        val d = driver()
        val you = d.activePlayer!!
        val fortification = d.putPermanentOnBattlefield(you, "Test Bastion")
        val land = d.putLandOnBattlefield(you, "Forest")
        d.giveColorlessMana(you, 3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)

        d.legalActions(you).any { (it.action as? ActivateAbility)?.abilityId == fortifyId } shouldBe false
        d.fortify(you, fortification, land).error shouldNotBe null
        d.hostOf(fortification) shouldBe null
    }

    test("CR 301.6: a Fortification can't be attached to a creature") {
        val d = driver()
        val you = d.activePlayer!!
        val fortification = d.putPermanentOnBattlefield(you, "Test Bastion")
        val land = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        AttachmentMover.canAttach(d.state, d.services.predicateEvaluator, d.cardRegistry, fortification, land) shouldBe true
        AttachmentMover.canAttach(d.state, d.services.predicateEvaluator, d.cardRegistry, fortification, bear) shouldBe false
    }

    test("CR 704.5n: a Fortification on a permanent that isn't a land unattaches and stays") {
        val d = driver()
        val you = d.activePlayer!!
        val fortification = d.putPermanentOnBattlefield(you, "Test Bastion")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.replaceState(AttachmentMover.attach(d.state, fortification, bear, you).first)
        d.hostOf(fortification) shouldBe bear

        val sba = d.services.sbaChecker.checkAndApply(d.state)
        sba.error shouldBe null
        d.replaceState(sba.state)

        d.hostOf(fortification) shouldBe null
        d.findPermanent(you, "Test Bastion") shouldBe fortification
    }
})
