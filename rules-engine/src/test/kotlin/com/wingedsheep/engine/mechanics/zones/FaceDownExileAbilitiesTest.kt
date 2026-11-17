package com.wingedsheep.engine.mechanics.zones

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * A card exiled face down has no characteristics (CR 406.3a): the exile-zone abilities its
 * definition prints aren't there, so `ZoneActivatedAbilityEnumerator` doesn't offer them and
 * `ActivationValidator` rejects them. An ability an effect granted that exiled card still works.
 */
class FaceDownExileAbilitiesTest : FunSpec({

    val exileGainer = card("Exile Gainer") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        oracleText = "{0}: You gain 2 life. Activate only if this card is in exile."
        activatedAbility {
            cost = Costs.Free
            effect = Effects.GainLife(2)
            activateFromZone = Zone.EXILE
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(exileGainer)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activations(source: EntityId) =
        legalActions(player1).filter { (it.action as? ActivateAbility)?.sourceId == source }

    fun GameTestDriver.turnFaceDown(id: EntityId) =
        replaceState(state.updateEntity(id) { it.with(FaceDownComponent) })

    val printedId = exileGainer.activatedAbilities.single().id

    test("face up in exile, the printed exile ability is offered and resolves") {
        val d = driver()
        val card = d.putCardInExile(d.player1, "Exile Gainer")
        d.activations(card) shouldHaveSize 1

        val life = d.getLifeTotal(d.player1)
        d.submitSuccess(ActivateAbility(d.player1, card, printedId))
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 2
    }

    test("face down in exile, the printed ability is neither offered nor accepted") {
        val d = driver()
        val card = d.putCardInExile(d.player1, "Exile Gainer")
        d.turnFaceDown(card)

        d.activations(card).shouldBeEmpty()
        val result = d.submitExpectFailure(ActivateAbility(d.player1, card, printedId))
        (result.error ?: "") shouldContain "face down"
        d.state.stack.shouldBeEmpty()
    }

    test("an ability an effect granted the face-down exiled card is still offered") {
        val d = driver()
        val card = d.putCardInExile(d.player1, "Grizzly Bears")
        d.turnFaceDown(card)
        val granted = ActivatedAbility(
            id = AbilityId("FaceDownExileAbilitiesTest_granted"),
            cost = Costs.Free,
            effect = Effects.GainLife(1),
            activateFromZone = Zone.EXILE,
        )
        d.replaceState(
            d.state.copy(
                grantedActivatedAbilities = d.state.grantedActivatedAbilities +
                    GrantedActivatedAbility(card, granted, Duration.Permanent)
            )
        )

        d.activations(card) shouldHaveSize 1
        d.submitSuccess(ActivateAbility(d.player1, card, granted.id))
    }
})
