package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.KitsuneBlademaster
import com.wingedsheep.mtg.sets.definitions.chk.cards.OathkeeperTakenosDaisho
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Oathkeeper, Takeno's Daisho (CHK #265) — "Equipped creature gets +3/+1. Whenever equipped
 * creature dies, return that card to the battlefield under your control if it's a Samurai card.
 * When Oathkeeper is put into a graveyard from the battlefield, exile equipped creature. Equip {2}"
 *
 * The load-bearing claims: the Samurai gate reads the dead card (a non-Samurai stays in the
 * graveyard), and the Equipment's own graveyard trigger still finds the creature it was attached
 * to after it has left the battlefield (last-known attachment).
 */
class OathkeeperTakenosDaishoScenarioTest : FunSpec({

    fun GameTestDriver.putEquipmentAttached(playerId: EntityId, cardName: String, host: EntityId): EntityId {
        val equipmentId = putPermanentOnBattlefield(playerId, cardName)
        var newState = state.updateEntity(equipmentId) { c -> c.with(AttachedToComponent(host)) }
        val existing = newState.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(host) { c -> c.with(AttachmentsComponent(existing + equipmentId)) }
        replaceState(newState)
        return equipmentId
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(OathkeeperTakenosDaisho, KitsuneBlademaster, Shatter))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.doomBlade(me: EntityId, host: EntityId) {
        giveMana(me, Color.BLACK, 2)
        val blade = putCardInHand(me, "Doom Blade")
        castSpellWithTargets(me, blade, listOf(ChosenTarget.Permanent(host)))
        bothPass() // Doom Blade resolves
        bothPass() // Oathkeeper's dies trigger resolves
    }

    test("equipped creature gets +3/+1") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Savannah Lions")
        d.putEquipmentAttached(me, "Oathkeeper, Takeno's Daisho", host)

        d.state.projectedState.getPower(host) shouldBe 4 // test-fixture Lions are 1/1
        d.state.projectedState.getToughness(host) shouldBe 2
    }

    test("an equipped Samurai that dies returns to the battlefield under your control") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Kitsune Blademaster")
        d.putEquipmentAttached(me, "Oathkeeper, Takeno's Daisho", host)

        d.doomBlade(me, host)

        withClue("the Samurai card came back") {
            d.findPermanent(me, "Kitsune Blademaster").shouldNotBeNull()
        }
        d.getGraveyardCardNames(me) shouldNotContain "Kitsune Blademaster"
    }

    test("an equipped non-Samurai that dies stays in the graveyard") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Savannah Lions")
        d.putEquipmentAttached(me, "Oathkeeper, Takeno's Daisho", host)

        d.doomBlade(me, host)

        d.findPermanent(me, "Savannah Lions") shouldBe null
        d.getGraveyardCardNames(me) shouldContain "Savannah Lions"
    }

    test("when Oathkeeper is put into a graveyard, the creature it equipped is exiled") {
        val d = driver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Kitsune Blademaster")
        val keeper = d.putEquipmentAttached(me, "Oathkeeper, Takeno's Daisho", host)

        d.giveMana(me, Color.RED, 2)
        val shatter = d.putCardInHand(me, "Shatter")
        d.castSpellWithTargets(me, shatter, listOf(ChosenTarget.Permanent(keeper)))
        d.bothPass() // Shatter resolves
        d.getGraveyard(me) shouldContain keeper
        d.bothPass() // Oathkeeper's graveyard trigger resolves

        d.findPermanent(me, "Kitsune Blademaster") shouldBe null
        d.getExileCardNames(me) shouldContain "Kitsune Blademaster"
    }
})
