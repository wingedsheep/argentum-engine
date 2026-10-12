package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.NumericKeywordValuesComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mechanics.squad
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TokenCopyDepartureCharacteristicsTest : FunSpec({
    val front = card("Test Numeric Squad Front") {
        manaCost = "{R}"
        typeLine = "Creature — Human Samurai"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))
        keywordAbility(KeywordAbility.bushido(2))
        squad("{1}")
    }
    val transformingFront = card("Test Squad Transforming Front") {
        manaCost = "{R}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        squad("{1}")
    }
    val transformingBack = card("Test Squad Transforming Back") {
        typeLine = "Creature — Werewolf"
        power = 3
        toughness = 3
    }
    val transforming = CardDefinition.doubleFacedCreature(transformingFront, transformingBack)
    val blink = card("Test Copy Snapshot Blink") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.Exile(creature) then Effects.PutOntoBattlefieldUnderYourControl(creature)
        }
    }
    fun setup(source: CardDefinition): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(source, transformingBack, blink))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (blinking in listOf(false, true)) {
        val departure = if (blinking) "blink" else "death"
        test("a triggering creature's numeric keywords survive $departure") {
            val d = setup(front)
            val player = d.player1
            d.giveMana(player, Color.RED)
            d.giveColorlessMana(player, 1)
            val source = d.putCardInHand(player, front.name)
            d.submit(CastSpell(player, source, paymentStrategy = PaymentStrategy.FromPool,
                declaredCostSlot = ChoiceSlot.SQUAD)).outcome shouldBe Outcome.Done
            d.bothPass()
            d.state.stack.size shouldBe 1

            d.giveMana(player, if (blinking) Color.BLUE else Color.RED)
            val response = d.putCardInHand(player, if (blinking) blink.name else "Lightning Bolt")
            d.castSpell(player, response, listOf(source)).outcome shouldBe Outcome.Done
            d.bothPass()
            if (blinking) {
                // A later object's numeric abilities must not overwrite the old departure snapshot.
                d.replaceState(d.state.updateEntity(source) {
                    it.with(ToxicComponent(9)).with(NumericKeywordValuesComponent(mapOf(Keyword.BUSHIDO to 9)))
                })
            }
            d.bothPass()

            val token = d.getPermanents(player).single { d.state.getEntity(it)!!.has<TokenComponent>() }
            val copied = d.state.getEntity(token)!!
            copied.get<ToxicComponent>()!!.amount shouldBe 1
            copied.get<NumericKeywordValuesComponent>()!!.values[Keyword.BUSHIDO] shouldBe 2
        }

        test("a transformed triggering creature retains both frozen faces after $departure") {
            val d = setup(transforming)
            val player = d.player1
            d.giveMana(player, Color.RED)
            d.giveColorlessMana(player, 1)
            val source = d.putCardInHand(player, transforming.name)
            d.submit(CastSpell(player, source, paymentStrategy = PaymentStrategy.FromPool,
                declaredCostSlot = ChoiceSlot.SQUAD)).outcome shouldBe Outcome.Done
            d.bothPass()
            val transformed = flipDfcInPlace(d.state, d.cardRegistry, source)
            transformed shouldNotBe null
            d.replaceState(transformed!!.first)
            d.state.getEntity(source)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.BACK

            d.giveMana(player, if (blinking) Color.BLUE else Color.RED)
            val response = d.putCardInHand(player, if (blinking) blink.name else "Lightning Bolt")
            d.castSpell(player, response, listOf(source)).outcome shouldBe Outcome.Done
            d.bothPass()
            if (blinking) {
                d.state.getEntity(source)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.FRONT
            }
            d.bothPass()

            val token = d.getPermanents(player).single { d.state.getEntity(it)!!.has<TokenComponent>() }
            val copied = d.state.getEntity(token)!!
            copied.get<CardComponent>()!!.name shouldBe transformingBack.name
            copied.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.BACK
            d.state.projectedState.getPower(token) shouldBe 3
            d.state.projectedState.getToughness(token) shouldBe 3
            val turnedBack = flipDfcInPlace(d.state, d.cardRegistry, token)
            turnedBack shouldNotBe null
            d.replaceState(turnedBack!!.first)
            d.state.getEntity(token)!!.get<CardComponent>()!!.name shouldBe transformingFront.name
            d.state.projectedState.getPower(token) shouldBe 2
            d.state.projectedState.getToughness(token) shouldBe 2
        }
    }
})
