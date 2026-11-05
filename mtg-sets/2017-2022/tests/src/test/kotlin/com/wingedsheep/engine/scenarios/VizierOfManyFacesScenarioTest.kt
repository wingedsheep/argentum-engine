package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.EmbalmedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.VizierOfManyFaces
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.embalmAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Vizier of Many Faces (AKH #74) — {2}{U}{U} 0/0 Shapeshifter Cleric.
 *
 *   You may have this creature enter as a copy of any creature on the battlefield, except if this
 *   creature was embalmed, the token has no mana cost, it's white, and it's a Zombie in addition to
 *   its other types.
 *   Embalm {3}{U}{U}
 *
 * Cast, it is a plain Clone. Embalmed, the token is a copy of the Vizier card, so it gets the copy
 * choice as it enters, and — being embalmed (CR 702.128b) — keeps embalm's white / Zombie / no mana
 * cost on top of what it copies.
 */
class VizierOfManyFacesScenarioTest : FunSpec({

    val embalmId = embalmAbility(ManaCost.parse("{3}{U}{U}")).id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(VizierOfManyFaces))
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val seenEvents = mutableListOf<GameEvent>()

    fun GameTestDriver.resolveChoosing(pick: (SelectCardsDecision) -> List<EntityId>): List<EntityId> {
        var offered: List<EntityId> = emptyList()
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 20) {
            when (val d = pendingDecision) {
                is SelectCardsDecision -> { offered = d.options; submitCardSelection(d.playerId, pick(d)) }
                null -> bothPass()
                else -> autoResolveDecision()
            }
            seenEvents += events
        }
        return offered
    }

    fun GameTestDriver.embalm(player: EntityId, vizier: EntityId) {
        giveMana(player, Color.BLUE, 2)
        giveColorlessMana(player, 3)
        submit(ActivateAbility(playerId = player, sourceId = vizier, abilityId = embalmId)).outcome shouldBe Outcome.Done
    }

    test("cast: enters as a plain copy of the chosen creature") {
        val d = driver()
        val me = d.player1
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val vizier = d.putCardInHand(me, "Vizier of Many Faces")
        d.giveMana(me, Color.BLUE, 2)
        d.giveColorlessMana(me, 2)
        d.castSpell(me, vizier).outcome shouldBe Outcome.Done
        d.resolveChoosing { listOf(courser) } shouldContain courser

        val projected = d.state.projectedState
        withClue("a cast Vizier isn't embalmed, so the copy is just Centaur Courser") {
            d.getCardName(vizier) shouldBe "Centaur Courser"
            projected.getColors(vizier) shouldBe setOf("GREEN")
            projected.getSubtypes(vizier) shouldNotContain "Zombie"
            d.state.getEntity(vizier)!!.get<CardComponent>()!!.manaValue shouldBe 3
        }
    }

    test("embalmed: the token chooses what to copy and stays a white Zombie with no mana cost") {
        val d = driver()
        val me = d.player1
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val vizier = d.putCardInGraveyard(me, "Vizier of Many Faces")
        d.embalm(me, vizier)
        val offered = d.resolveChoosing { listOf(courser) }
        withClue("the embalmed token is a copy of the Vizier card, so it gets the as-enters copy choice") {
            offered shouldContain courser
        }

        val token = d.findPermanent(me, "Centaur Courser").shouldNotBeNull()
        val entity = d.state.getEntity(token)!!
        val projected = d.state.projectedState
        withClue("embalmed token copying Centaur Courser keeps the restated embalm exceptions") {
            entity.has<TokenComponent>() shouldBe true
            entity.has<EmbalmedComponent>() shouldBe true
            projected.getColors(token) shouldBe setOf("WHITE")
            projected.getSubtypes(token) shouldContain "Zombie"
            projected.getSubtypes(token) shouldContain "Centaur"
            entity.get<CardComponent>()!!.manaValue shouldBe 0
            projected.getPower(token) shouldBe 3
            projected.getToughness(token) shouldBe 3
        }
        d.getExileCardNames(me) shouldContain "Vizier of Many Faces"
    }

    test("embalmed, copying nothing: the token is a 0/0 Vizier and dies") {
        val d = driver()
        val me = d.player1
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val vizier = d.putCardInGraveyard(me, "Vizier of Many Faces")
        d.embalm(me, vizier)
        seenEvents.clear()
        val offered = d.resolveChoosing { emptyList() }
        offered shouldContain courser
        val tokenEntered = seenEvents.any {
            it is ZoneChangeEvent && it.entityName == "Vizier of Many Faces" && it.toZone == Zone.BATTLEFIELD
        }
        withClue("the declined token entered as a 0/0 Vizier and died to state-based actions") {
            tokenEntered shouldBe true
            d.findPermanent(me, "Vizier of Many Faces") shouldBe null
            d.getExileCardNames(me) shouldContain "Vizier of Many Faces"
        }
    }
})
