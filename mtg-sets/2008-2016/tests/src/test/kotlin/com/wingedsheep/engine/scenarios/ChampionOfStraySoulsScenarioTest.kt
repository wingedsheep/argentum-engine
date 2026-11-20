package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.ChampionOfStraySouls
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Champion of Stray Souls — "{3}{B}{B}, {T}, Sacrifice X other creatures: Return X target creature
 * cards from your graveyard to the battlefield. / {5}{B}{B}: Put this card from your graveyard on
 * top of your library."
 *
 * The sacrifice pick announces X (CR 601.2b) and the target choice that follows is sized to exactly
 * X. Targets are chosen before the cost is paid (CR 601.2c, 601.2h), so the creatures being
 * sacrificed can't be among them.
 */
class ChampionOfStraySoulsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChampionOfStraySouls))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val reanimateId = ChampionOfStraySouls.activatedAbilities.first { it.targetRequirements.isNotEmpty() }.id
    val toLibraryId = ChampionOfStraySouls.activatedAbilities.first { it.targetRequirements.isEmpty() }.id

    test("sacrificing two creatures asks for exactly two creature cards and returns them") {
        val d = createDriver()
        val you = d.activePlayer!!
        val champion = d.putCreatureOnBattlefield(you, "Champion of Stray Souls")
        d.removeSummoningSickness(champion)
        val bear = d.putCreatureOnBattlefield(you, "Goblin Guide")
        val lions = d.putCreatureOnBattlefield(you, "Savannah Lions")
        val courser = d.putCardInGraveyard(you, "Centaur Courser")
        val ogre = d.putCardInGraveyard(you, "Force of Nature")
        d.giveMana(you, Color.BLACK, 5)

        d.submit(ActivateAbility(you, champion, reanimateId))
        val sacrifice = d.pendingDecision
        sacrifice.shouldBeInstanceOf<SelectCardsDecision>()
        sacrifice.options.contains(champion) shouldBe false
        d.submitDecision(you, CardsSelectedResponse(sacrifice.id, listOf(bear, lions)))

        val targets = d.pendingDecision
        targets.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().minTargets shouldBe 2
        targets.targetRequirements.single().maxTargets shouldBe 2
        // Targets are chosen before the cost is paid (CR 601.2c, 601.2h): the creatures being
        // sacrificed are still on the battlefield, so they can't be returned by this activation.
        targets.legalTargets[0]!!.shouldContainExactlyInAnyOrder(courser, ogre)
        d.submitDecision(you, TargetsResponse(targets.id, mapOf(0 to listOf(courser, ogre))))

        d.bothPass()
        val battlefield = d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD))
        battlefield.contains(courser) shouldBe true
        battlefield.contains(ogre) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).shouldContainExactlyInAnyOrder(bear, lions)
    }

    test("sacrificing one creature asks for exactly one target") {
        val d = createDriver()
        val you = d.activePlayer!!
        val champion = d.putCreatureOnBattlefield(you, "Champion of Stray Souls")
        d.removeSummoningSickness(champion)
        val bear = d.putCreatureOnBattlefield(you, "Goblin Guide")
        d.putCreatureOnBattlefield(you, "Savannah Lions")
        val courser = d.putCardInGraveyard(you, "Centaur Courser")
        d.putCardInGraveyard(you, "Force of Nature")
        d.giveMana(you, Color.BLACK, 5)

        d.submit(ActivateAbility(you, champion, reanimateId))
        val sacrifice = d.pendingDecision
        sacrifice.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(you, CardsSelectedResponse(sacrifice.id, listOf(bear)))

        val targets = d.pendingDecision
        targets.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().minTargets shouldBe 1
        targets.targetRequirements.single().maxTargets shouldBe 1
        d.submitDecision(you, TargetsResponse(targets.id, mapOf(0 to listOf(courser))))

        d.bothPass()
        d.state.getZone(ZoneKey(you, Zone.BATTLEFIELD)).contains(courser) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(bear) shouldBe true
    }

    test("from the graveyard it puts itself on top of its owner's library") {
        val d = createDriver()
        val you = d.activePlayer!!
        val champion = d.putCardInGraveyard(you, "Champion of Stray Souls")
        d.giveMana(you, Color.BLACK, 7)

        d.submit(ActivateAbility(you, champion, toLibraryId)).error shouldBe null
        d.bothPass()
        d.state.getZone(ZoneKey(you, Zone.LIBRARY)).first() shouldBe champion
    }
})
