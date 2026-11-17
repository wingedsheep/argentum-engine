package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.AerieBowmasters
import com.wingedsheep.mtg.sets.definitions.dtk.cards.DeathmistRaptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deathmist Raptor (DTK #180) — "Whenever a permanent you control is turned face up, you may
 * return this card from your graveyard to the battlefield face up or face down. Megamorph {4}{G}"
 *
 * Pins what the script can't prove on its own: the turned-face-up trigger functions from the
 * graveyard, the face-up / face-down / stay choice is made on resolution, the face-down return is
 * a 2/2 that turns up for the Raptor's own megamorph cost (with its +1/+1 counter), and an
 * opponent's flip doesn't trigger it.
 */
class DeathmistRaptorScenarioTest : FunSpec({

    val allCards = TestCards.all + listOf(DeathmistRaptor, AerieBowmasters)

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(allCards)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().first()
        replaceState(state.updateEntity(creatureId) { container ->
            container
                .with(FaceDownComponent)
                .with(MorphDataComponent(morphAbility.morphCost, cardDef.name, morphAbility.faceUpEffect))
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    /** Turn a face-down Aerie Bowmasters up for its megamorph {5}{G}. */
    fun GameTestDriver.flipBowmasters(playerId: EntityId, bowmasters: EntityId) {
        giveColorlessMana(playerId, 5)
        giveMana(playerId, Color.GREEN, 1)
        submit(TurnFaceUp(playerId = playerId, sourceId = bowmasters, costTargetIds = emptyList()))
            .error shouldBe null
    }

    fun GameTestDriver.resolveRaptorTriggerChoosing(label: String) {
        stackSize shouldBe 1
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(label)
        withClue("expected '$label' in ${decision.options}") { index shouldNotBe -1 }
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    test("returns face up as a 3/3 deathtouch creature when a permanent you control turns face up") {
        val d = createDriver()
        val me = d.player1
        d.putCardInGraveyard(me, "Deathmist Raptor")
        val bowmasters = d.putFaceDownCreature(me, "Aerie Bowmasters")

        d.flipBowmasters(me, bowmasters)
        d.resolveRaptorTriggerChoosing("Return Deathmist Raptor face up")

        d.getGraveyardCardNames(me) shouldNotContain "Deathmist Raptor"
        val raptor = d.findPermanent(me, "Deathmist Raptor")
        raptor shouldNotBe null
        d.state.getEntity(raptor!!)!!.has<FaceDownComponent>() shouldBe false
        d.state.projectedState.getPower(raptor) shouldBe 3
        d.state.projectedState.getToughness(raptor) shouldBe 3
    }

    test("returns face down as a 2/2 that turns up for megamorph {4}{G} with a +1/+1 counter") {
        val d = createDriver()
        val me = d.player1
        d.putCardInGraveyard(me, "Deathmist Raptor")
        val bowmasters = d.putFaceDownCreature(me, "Aerie Bowmasters")

        d.flipBowmasters(me, bowmasters)
        d.resolveRaptorTriggerChoosing("Return Deathmist Raptor face down")

        d.getGraveyardCardNames(me) shouldNotContain "Deathmist Raptor"
        val faceDown = d.getPermanents(me).singleOrNull { d.state.getEntity(it)!!.has<FaceDownComponent>() }
        withClue("the Raptor came back face down") { faceDown shouldNotBe null }
        d.state.projectedState.getPower(faceDown!!) shouldBe 2
        d.state.projectedState.getToughness(faceDown) shouldBe 2

        d.giveColorlessMana(me, 4)
        d.giveMana(me, Color.GREEN, 1)
        d.submit(TurnFaceUp(playerId = me, sourceId = faceDown, costTargetIds = emptyList())).error shouldBe null

        d.state.getEntity(faceDown)!!.has<FaceDownComponent>() shouldBe false
        d.findPermanent(me, "Deathmist Raptor") shouldBe faceDown
        d.state.getEntity(faceDown)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.getPower(faceDown) shouldBe 4
        d.state.projectedState.getToughness(faceDown) shouldBe 4
    }

    test("may leave it in the graveyard") {
        val d = createDriver()
        val me = d.player1
        d.putCardInGraveyard(me, "Deathmist Raptor")
        val bowmasters = d.putFaceDownCreature(me, "Aerie Bowmasters")

        d.flipBowmasters(me, bowmasters)
        d.resolveRaptorTriggerChoosing("Leave it in your graveyard")

        d.getGraveyardCardNames(me) shouldContain "Deathmist Raptor"
        d.findPermanent(me, "Deathmist Raptor") shouldBe null
    }

    test("an opponent's permanent turning face up does not trigger it — 'you control'") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        d.putCardInGraveyard(opponent, "Deathmist Raptor")
        val bowmasters = d.putFaceDownCreature(me, "Aerie Bowmasters")

        d.flipBowmasters(me, bowmasters)

        d.stackSize shouldBe 0
        d.getGraveyardCardNames(opponent) shouldContain "Deathmist Raptor"
    }
})
