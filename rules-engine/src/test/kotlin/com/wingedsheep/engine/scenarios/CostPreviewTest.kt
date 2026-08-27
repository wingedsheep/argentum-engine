package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The read-only cost preview: what a *draft* cast or activation costs given the choices made so
 * far, straight from the handler's own cost chain. The client shows this instead of its own
 * arithmetic, so the invariants here are the ones the HUDs lean on:
 *
 * - it credits every payment in the draft exactly as `validate` prices it (convoke, delve,
 *   improvise, the per-target tax, an announced X);
 * - its verdict on an illegal choice is the same message `validate` would give;
 * - it never changes the game.
 */
class CostPreviewTest : FunSpec({

    val fireball = card("Preview Fireball") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        oracleText = "Preview Fireball deals X damage to any target."
        spell {
            target = com.wingedsheep.sdk.dsl.Targets.Any
            effect = Effects.DealDamage(
                com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue,
                com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0),
            )
        }
    }

    val improvisingBlueprint = card("Preview Blueprint") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery"
        oracleText = "Improvise\nYou gain 5 life."
        keywords(Keyword.IMPROVISE)
        spell { effect = Effects.GainLife(5) }
    }

    val trinket = card("Preview Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(fireball, improvisingBlueprint, trinket))
        it.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 8,
                "Island" to 8,
                "Swamp" to 4,
                "Mountain" to 4,
                "Merrow Skyswimmer" to 2,
                "Savannah Lions" to 2,
                "Phantom Warrior" to 2,
            )
        )
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun draft(player: EntityId, cardId: EntityId, payment: AlternativePaymentChoice? = null, xValue: Int? = null, targets: List<ChosenTarget> = emptyList()) =
        CastSpell(
            playerId = player,
            cardId = cardId,
            targets = targets,
            xValue = xValue,
            paymentStrategy = PaymentStrategy.AutoPay,
            alternativePayment = payment,
        )

    context("convoke") {
        test("each tapped creature is credited against its colour, and the rest is what's left to pay") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val skyswimmer = driver.putCardInHand(player, "Merrow Skyswimmer") // {3}{W/U}{W/U}
            val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")
            val warrior = driver.putCreatureOnBattlefield(player, "Phantom Warrior")
            driver.removeSummoningSickness(lions)
            driver.removeSummoningSickness(warrior)
            repeat(3) { driver.putLandOnBattlefield(player, "Plains") }

            val untouched = driver.previewCost(draft(player, skyswimmer)).shouldNotBeNull()
            untouched.manaCostString shouldBe "{3}{W/U}{W/U}"
            untouched.genericRemaining shouldBe 3

            val convoked = driver.previewCost(
                draft(
                    player, skyswimmer,
                    AlternativePaymentChoice(
                        convokedCreatures = mapOf(
                            lions to ConvokePayment(Color.WHITE),
                            warrior to ConvokePayment(Color.BLUE),
                        )
                    )
                )
            ).shouldNotBeNull()
            convoked.manaCostString shouldBe "{3}"
            convoked.genericRemaining shouldBe 3
            convoked.affordable shouldBe true
            convoked.error shouldBe null
            // Three Plains cover the {3}; the engine names them.
            convoked.autoTapPreview.shouldNotBeNull().size shouldBe 3
        }

        test("a wrong colour is the same rejection validate gives, and the board is untouched") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val skyswimmer = driver.putCardInHand(player, "Merrow Skyswimmer")
            val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")
            driver.removeSummoningSickness(lions)
            repeat(5) { driver.putLandOnBattlefield(player, "Plains") }

            val preview = driver.previewCost(
                draft(player, skyswimmer, AlternativePaymentChoice(convokedCreatures = mapOf(lions to ConvokePayment(Color.BLUE))))
            ).shouldNotBeNull()
            preview.affordable shouldBe false
            preview.error.shouldNotBeNull() shouldContain "Savannah Lions can't pay blue mana"
            preview.autoTapPreview shouldBe null
            driver.isTapped(lions) shouldBe false
            driver.stackSize shouldBe 0
        }

        test("unaffordable stays unaffordable until enough is tapped") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val skyswimmer = driver.putCardInHand(player, "Merrow Skyswimmer")
            val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")
            driver.removeSummoningSickness(lions)
            repeat(3) { driver.putLandOnBattlefield(player, "Plains") }

            val before = driver.previewCost(draft(player, skyswimmer)).shouldNotBeNull()
            before.affordable shouldBe false
            before.error.shouldNotBeNull() shouldContain "Not enough mana"

            val after = driver.previewCost(
                draft(player, skyswimmer, AlternativePaymentChoice(convokedCreatures = mapOf(lions to ConvokePayment(Color.WHITE))))
            ).shouldNotBeNull()
            // {3}{W/U}{W/U} minus a white pip → {3}{W/U}, and a Plains still pays the hybrid.
            after.manaCostString shouldBe "{3}{W/U}"
            after.affordable shouldBe false // 3 Plains for 4 mana
            driver.putLandOnBattlefield(player, "Plains")
            driver.previewCost(
                draft(player, skyswimmer, AlternativePaymentChoice(convokedCreatures = mapOf(lions to ConvokePayment(Color.WHITE))))
            ).shouldNotBeNull().affordable shouldBe true
        }
    }

    context("delve") {
        test("each exiled card pays one generic; the cap the client enforces is what's left") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val angler = driver.putCardInHand(player, "Gurmag Angler") // {6}{B}
            val graveyard = (1..3).map { driver.putCardInGraveyard(player, "Plains") }
            driver.putLandOnBattlefield(player, "Swamp")

            val two = driver.previewCost(draft(player, angler, AlternativePaymentChoice(delvedCards = graveyard.take(2)))).shouldNotBeNull()
            two.manaCostString shouldBe "{4}{B}"
            two.genericRemaining shouldBe 4
            two.affordable shouldBe false

            val three = driver.previewCost(draft(player, angler, AlternativePaymentChoice(delvedCards = graveyard))).shouldNotBeNull()
            three.manaCostString shouldBe "{3}{B}"
            driver.getGraveyard(player).size shouldBe 3 // nothing was exiled
        }
    }

    context("improvise") {
        test("a tapped artifact pays one generic; over-tapping buys nothing more") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val blueprint = driver.putCardInHand(player, "Preview Blueprint") // {4}{U}
            val artifacts = (1..5).map { driver.putPermanentOnBattlefield(player, "Preview Trinket") }
            driver.putLandOnBattlefield(player, "Island")

            val four = driver.previewCost(
                draft(player, blueprint, AlternativePaymentChoice(tapForGenericPermanents = artifacts.take(4).toSet()))
            ).shouldNotBeNull()
            four.manaCostString shouldBe "{U}"
            four.genericRemaining shouldBe 0
            four.affordable shouldBe true

            val five = driver.previewCost(
                draft(player, blueprint, AlternativePaymentChoice(tapForGenericPermanents = artifacts.toSet()))
            ).shouldNotBeNull()
            five.manaCostString shouldBe "{U}"
            five.genericRemaining shouldBe 0
        }
    }

    context("X") {
        test("an announced X folds into the generic, and affordability follows it") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val ball = driver.putCardInHand(player, "Preview Fireball")
            repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
            val opponent = driver.getOpponent(player)

            val x2 = driver.previewCost(draft(player, ball, xValue = 2, targets = listOf(ChosenTarget.Player(opponent)))).shouldNotBeNull()
            x2.manaCostString shouldBe "{2}{R}"
            x2.xValue shouldBe 2
            x2.affordable shouldBe true
            x2.autoTapPreview.shouldNotBeNull().size shouldBe 3

            val x3 = driver.previewCost(draft(player, ball, xValue = 3, targets = listOf(ChosenTarget.Player(opponent)))).shouldNotBeNull()
            x3.manaCostString shouldBe "{3}{R}"
            x3.affordable shouldBe false
        }
    }

    context("activated abilities") {
        test("a {T} ability owes no mana and names no sources") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val birds = driver.putCreatureOnBattlefield(player, "Birds of Paradise")
            driver.removeSummoningSickness(birds)
            val ability = TestCards.BirdsOfParadise.script.activatedAbilities.single()

            val preview = driver.previewCost(
                ActivateAbility(playerId = player, sourceId = birds, abilityId = ability.id)
            ).shouldNotBeNull()
            preview.manaCostString shouldBe ""
            preview.affordable shouldBe true
            preview.autoTapPreview shouldBe null
        }

        test("an unknown ability is reported, not thrown") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")
            val preview = driver.previewCost(
                ActivateAbility(playerId = player, sourceId = lions, abilityId = com.wingedsheep.sdk.scripting.AbilityId("nope"))
            ).shouldNotBeNull()
            preview.affordable shouldBe false
            preview.error shouldBe "Ability not found on this card"
        }
    }

    test("a card that isn't there is reported, not thrown") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val preview = driver.previewCost(draft(player, EntityId("missing"))).shouldNotBeNull()
        preview.affordable shouldBe false
        preview.error shouldBe "Card not found"
    }

    test("only casts and activations have a cost to preview") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.previewCost(com.wingedsheep.engine.core.PassPriority(player)) shouldBe null
    }

    test("a preview names the same sources auto-pay would tap") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val lions = driver.putCardInHand(player, "Savannah Lions") // {W}
        val plains = driver.putLandOnBattlefield(player, "Plains")
        driver.putLandOnBattlefield(player, "Island")
        driver.previewCost(draft(player, lions)).shouldNotBeNull().autoTapPreview
            .shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(plains)
    }
})
