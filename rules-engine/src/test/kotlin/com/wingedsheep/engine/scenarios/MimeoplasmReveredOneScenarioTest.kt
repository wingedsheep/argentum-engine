package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dft.cards.MimeoplasmReveredOne
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MimeoplasmReveredOneScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(MimeoplasmReveredOne)
        it.initMirrorMatch(Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("as it enters, exiles up to X graveyard creatures and gets three counters for each") {
        val d = driver()
        val player = d.activePlayer!!
        val bear = d.putCardInGraveyard(player, "Grizzly Bears")
        val giant = d.putCardInGraveyard(player, "Hill Giant")
        val mimeoplasm = d.putCardInHand(player, "Mimeoplasm, Revered One")
        d.giveMana(player, Color.BLACK, 2)
        d.giveMana(player, Color.GREEN, 2)
        d.giveMana(player, Color.BLUE, 2)

        d.castXSpell(player, mimeoplasm, xValue = 2).error shouldBe null
        d.bothPass()
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain bear
        decision.options shouldContain giant
        decision.maxSelections shouldBe 2
        d.submitCardSelection(player, listOf(bear, giant)).error shouldBe null

        d.state.getBattlefield() shouldContain mimeoplasm
        d.state.getGraveyard(player) shouldNotContain bear
        d.state.getGraveyard(player) shouldNotContain giant
        d.state.getEntity(mimeoplasm)!!.get<LinkedExileComponent>()!!.exiledIds shouldBe listOf(bear, giant)
        d.state.getEntity(mimeoplasm)!!.get<CountersComponent>()!!
            .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 6
    }

    test("the as-enters choice may be declined") {
        val d = driver()
        val player = d.activePlayer!!
        val bear = d.putCardInGraveyard(player, "Grizzly Bears")
        val mimeoplasm = d.putCardInHand(player, "Mimeoplasm, Revered One")
        d.giveMana(player, Color.BLACK, 2)
        d.giveMana(player, Color.GREEN, 2)
        d.giveMana(player, Color.BLUE, 2)

        d.castXSpell(player, mimeoplasm, xValue = 1).error shouldBe null
        d.bothPass()
        d.submitCardSelection(player, emptyList()).error shouldBe null

        d.state.getGraveyard(player) shouldContain bear
        d.state.getBattlefield() shouldNotContain mimeoplasm // it enters as 0/0 and dies
    }

    test("copy ability targets only a card exiled with it and keeps 0/0, counters, and this ability") {
        val d = driver()
        val player = d.activePlayer!!
        val bear = d.putCardInGraveyard(player, "Grizzly Bears")
        val unrelated = d.putCardInGraveyard(player, "Hill Giant")
        val mimeoplasm = d.putCardInHand(player, "Mimeoplasm, Revered One")
        d.giveMana(player, Color.BLACK, 3)
        d.giveMana(player, Color.GREEN, 3)
        d.giveMana(player, Color.BLUE, 3)

        d.castXSpell(player, mimeoplasm, xValue = 1).error shouldBe null
        d.bothPass()
        d.submitCardSelection(player, listOf(bear)).error shouldBe null

        val ability = MimeoplasmReveredOne.activatedAbilities.single()
        val activate = d.submit(ActivateAbility(
            playerId = player,
            sourceId = mimeoplasm,
            abilityId = ability.id,
            targets = listOf(ChosenTarget.Card(bear, player, Zone.EXILE))
        ))
        activate.error shouldBe null
        d.bothPass()

        d.state.getEntity(mimeoplasm)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
        d.state.projectedState.getPower(mimeoplasm) shouldBe 3
        d.state.projectedState.getToughness(mimeoplasm) shouldBe 3
        d.state.grantedActivatedAbilities.count { it.entityId == mimeoplasm && it.ability.id == ability.id } shouldBe 1

        val illegal = d.submit(ActivateAbility(
            playerId = player,
            sourceId = mimeoplasm,
            abilityId = ability.id,
            targets = listOf(ChosenTarget.Card(unrelated, player, Zone.GRAVEYARD))
        ))
        (illegal.error != null) shouldBe true
    }
})
