package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.GemOfBecoming
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gem of Becoming — "{3}, {T}, Sacrifice this artifact: Search your library for an Island card, a
 * Swamp card, and a Mountain card. Reveal those cards, put them into your hand, then shuffle."
 *
 * Three up-to-one picks over one search; each pick draws from the previous pick's remainder, so a
 * dual-typed card fills only one slot.
 */
class GemOfBecomingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GemOfBecoming))
        driver.initMirrorMatch(deck = Deck.of("Centaur Courser" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val abilityId = GemOfBecoming.activatedAbilities.single().id

    test("finds one Island, one Swamp and one Mountain card and puts them into hand") {
        val d = createDriver()
        val you = d.activePlayer!!
        val gem = d.putPermanentOnBattlefield(you, "Gem of Becoming")
        val island = d.putCardOnTopOfLibrary(you, "Island")
        val swamp = d.putCardOnTopOfLibrary(you, "Swamp")
        val mountain = d.putCardOnTopOfLibrary(you, "Mountain")
        d.giveColorlessMana(you, 3)

        d.submit(ActivateAbility(you, gem, abilityId)).error shouldBe null
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)) shouldContain gem
        d.bothPass()

        for ((expected, others) in listOf(island to listOf(swamp, mountain), swamp to listOf(island, mountain), mountain to listOf(island, swamp))) {
            val pick = d.pendingDecision
            pick.shouldBeInstanceOf<SelectCardsDecision>()
            pick.options shouldContain expected
            others.forEach { pick.options shouldNotContain it }
            d.submitDecision(you, CardsSelectedResponse(pick.id, listOf(expected)))
        }

        d.state.getZone(ZoneKey(you, Zone.HAND)).filter { it in listOf(island, swamp, mountain) }
            .shouldContainExactlyInAnyOrder(island, swamp, mountain)
    }

    test("a card with two of the types fills only one slot") {
        val d = createDriver()
        val you = d.activePlayer!!
        val gem = d.putPermanentOnBattlefield(you, "Gem of Becoming")
        val sea = d.putCardOnTopOfLibrary(you, "Underground Sea")
        val mountain = d.putCardOnTopOfLibrary(you, "Mountain")
        d.giveColorlessMana(you, 3)

        d.submit(ActivateAbility(you, gem, abilityId)).error shouldBe null
        d.bothPass()

        val islandPick = d.pendingDecision
        islandPick.shouldBeInstanceOf<SelectCardsDecision>()
        islandPick.options shouldContain sea
        d.submitDecision(you, CardsSelectedResponse(islandPick.id, listOf(sea)))

        // The Sea already answered the Island slot, so the Swamp pick can't take it again.
        var pick = d.pendingDecision
        pick.shouldBeInstanceOf<SelectCardsDecision>()
        if (mountain !in pick.options) {
            pick.options shouldNotContain sea
            d.submitDecision(you, CardsSelectedResponse(pick.id, emptyList()))
            pick = d.pendingDecision
            pick.shouldBeInstanceOf<SelectCardsDecision>()
        }
        pick.options shouldContain mountain
        pick.options shouldNotContain sea
        d.submitDecision(you, CardsSelectedResponse(pick.id, listOf(mountain)))

        val hand = d.state.getZone(ZoneKey(you, Zone.HAND))
        hand shouldContain sea
        hand shouldContain mountain
        d.state.getZone(ZoneKey(you, Zone.LIBRARY)) shouldNotContain sea
    }
})
