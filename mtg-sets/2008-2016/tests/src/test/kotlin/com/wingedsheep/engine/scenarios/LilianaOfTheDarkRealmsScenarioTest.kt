package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.EmblemActivatedAbilityComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.LilianaOfTheDarkRealms
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Liliana of the Dark Realms {2}{B}{B} — Legendary Planeswalker — Liliana (loyalty 3)
 *   +1: Search your library for a Swamp card, reveal it, put it into your hand, then shuffle.
 *   −3: Target creature gets +X/+X or -X/-X until end of turn, where X is the number of Swamps you control.
 *   −6: You get an emblem with "Swamps you control have '{T}: Add {B}{B}{B}{B}.'"
 *
 * The −3's sign is picked as it resolves, so both directions are driven. The −6's grant is
 * tapped by hand here; `EmblemGrantedManaAbilityTest` pins the auto-payer's side.
 */
class LilianaOfTheDarkRealmsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LilianaOfTheDarkRealms))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putLiliana(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val liliana = driver.putPermanentOnBattlefield(playerId, "Liliana of the Dark Realms")
        driver.addComponent(liliana, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return liliana
    }

    val abilities = LilianaOfTheDarkRealms.script.activatedAbilities

    fun GameTestDriver.resolveMinusThree(me: EntityId, liliana: EntityId, creature: EntityId, label: String) {
        submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = liliana,
                abilityId = abilities[1].id,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        )
        bothPass()
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(me, OptionChosenResponse(decision.id, decision.options.indexOf(label)))
    }

    test("+1 finds a Swamp card — and only a Swamp card — and puts it into your hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val liliana = putLiliana(driver, me, 3)
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val handBefore = driver.getHandSize(me)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = liliana, abilityId = abilities[0].id))
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("a non-Swamp isn't offered") { decision.options shouldNotContain bears }
        driver.submitDecision(me, CardsSelectedResponse(decision.id, listOf(decision.options.first())))

        driver.getHandSize(me) shouldBe handBefore + 1
        driver.state.getEntity(liliana)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
    }

    test("−3 choosing -X/-X shrinks the target by the number of Swamps you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }
        driver.putLandOnBattlefield(opp, "Swamp") // the opponent's Swamps don't count
        val liliana = putLiliana(driver, me, 3)
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.resolveMinusThree(me, liliana, courser, "-X/-X")

        driver.state.projectedState.getPower(courser) shouldBe 1
        driver.state.projectedState.getToughness(courser) shouldBe 1
    }

    test("−3 choosing +X/+X grows the target by the number of Swamps you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(me, "Swamp") }
        val liliana = putLiliana(driver, me, 3)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.resolveMinusThree(me, liliana, bears, "+X/+X")

        driver.state.projectedState.getPower(bears) shouldBe 5
        driver.state.projectedState.getToughness(bears) shouldBe 5
    }

    test("−6 emblem: a Swamp you control taps for {B}{B}{B}{B}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val liliana = putLiliana(driver, me, 6)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = liliana, abilityId = abilities[2].id))
        driver.bothPass()

        val grantIds = driver.state.entities.values
            .mapNotNull { it.get<EmblemActivatedAbilityComponent>() }
            .flatMap { grant -> grant.abilities.map { it.id } }
        withClue("the emblem exists") { grantIds.size shouldBe 1 }

        val swamp = driver.putLandOnBattlefield(me, "Swamp")
        val tap = driver.legalActions(me).map { it.action }.filterIsInstance<ActivateAbility>()
            .single { it.sourceId == swamp && it.abilityId in grantIds }
        driver.submitSuccess(tap)
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.black shouldBe 4
    }
})
