package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.composite.RollDiceExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.RollAdditionalDice
import com.wingedsheep.sdk.scripting.effects.RollDiceEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RollDiceEffectTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Island" to 40))
    }
    fun executor(d: GameTestDriver) = RollDiceExecutor(PredicateEvaluator(d.cardRegistry).conditions.amounts, d.cardRegistry)

    test("seeded dice advance only the returned RNG, count overlapping result sets, and emit public events") {
        val d = setup()
        val original = d.state.copy(rng = GameRng.seeded(12345))
        val context = EffectContext(null, d.activePlayer!!)
        val effect = RollDiceEffect(DynamicAmount.Fixed(30), 6, resultCounts = mapOf("even" to setOf(2,4,6), "sixes" to setOf(6), "none" to setOf(9)))
        val a = executor(d).execute(original, effect, context)
        val b = executor(d).execute(original, effect, context)
        a shouldBe b
        original.rng shouldBe GameRng.seeded(12345)
        val rolls = a.events.filterIsInstance<DieRolledEvent>()
        var expectedRng = original.rng
        val expectedResults = List(30) {
            val (face, next) = expectedRng.nextInt(6)
            expectedRng = next
            face + 1
        }
        rolls.map { it.result } shouldBe expectedResults
        a.state.rng shouldBe expectedRng
        rolls.size shouldBe 30
        rolls.all { it.result in 1..6 && it.playerId == context.controllerId } shouldBe true
        a.updatedStoredNumbers shouldBe mapOf("even" to rolls.count { it.result % 2 == 0 }, "sixes" to rolls.count { it.result == 6 }, "none" to 0)
        rolls.all { it.revealsInformation } shouldBe true
        val dto = com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(rolls.first()), context.controllerId).single()
        (dto as ClientEvent.DieRolled).result shouldBe rolls.first().result
    }
    test("zero and negative counts write zero tallies without consuming randomness or emitting events") {
        val d=setup(); val context=EffectContext(null,d.activePlayer!!)
        for (n in listOf(0,-3)) {
            val result=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(n),resultCounts=mapOf("sixes" to setOf(6))),context)
            result.state shouldBe d.state
            result.events shouldBe emptyList()
            result.updatedStoredNumbers shouldBe mapOf("sixes" to 0)
        }
    }
    test("any positive die size works and a referenced player receives the rolls") {
        val d=setup(); val opponent=d.getOpponent(d.activePlayer!!)
        for (sides in listOf(1,20)) {
            val result=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(20),sides,EffectTarget.SpecificEntity(opponent)),EffectContext(null,d.activePlayer!!))
            val rolls = result.events.filterIsInstance<DieRolledEvent>()
            rolls.size shouldBe 20
            rolls.all { it.playerId == opponent && it.result in 1..sides } shouldBe true
        }
    }
    test("numeric result context is frozen independently of later rolls") {
        val d=setup(); val event=DieRolledEvent(d.activePlayer!!,20,19)
        val ctx=EffectContext(null,d.activePlayer!!,triggerContext=TriggerContext.fromEvent(event))
        executor(d).execute(d.state,RollDiceEffect(),ctx)
        PredicateEvaluator(d.cardRegistry).conditions.amounts.evaluate(d.state,DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DIE_RESULT),ctx) shouldBe 19
        ctx.triggerContext!!.triggeringPlayerId shouldBe d.activePlayer
    }
    test("a die observer triggers once per matching die, for its roller, and survives source departure") {
        val observer=card("Test Die Observer") {
            typeLine="Enchantment"
            triggeredAbility {
                trigger=Triggers.you.rollsDie(minimumResult=4,maximumResult=6)
                effect=Effects.GainLife(DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DIE_RESULT))
            }
        }
        val roller=card("Test Die Roller") {
            typeLine="Artifact"
            activatedAbility { cost=Costs.Tap; effect=Effects.RollDice(DynamicAmount.Fixed(10),6) then Effects.SacrificeTarget(EffectTarget.Self) }
        }
        val d=setup(); d.registerCards(listOf(observer,roller));d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player=d.activePlayer!!;val opponent=d.getOpponent(player)
        d.putPermanentOnBattlefield(player,observer.name)
        d.putPermanentOnBattlefield(opponent,observer.name)
        val source=d.putPermanentOnBattlefield(player,roller.name)
        d.replaceState(d.state.copy(rng=GameRng.seeded(13)))
        d.submit(ActivateAbility(player,source,roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        val resolved=d.bothPass()
        val matches=resolved.events.filterIsInstance<DieRolledEvent>().filter { it.result in 4..6 }
        (matches.isNotEmpty()) shouldBe true
        d.state.stack.size shouldBe matches.size
        (source in d.getPermanents(player)) shouldBe false
        val life=d.getLifeTotal(player)
        repeat(matches.size) { d.bothPass() }
        d.getLifeTotal(player) shouldBe life+matches.sumOf { it.result }
        d.getLifeTotal(opponent) shouldBe 20
    }
    test("each roll instruction closes with one DiceRolledEvent per roller carrying all its results") {
        val d=setup(); val player=d.activePlayer!!; val opponent=d.getOpponent(player)
        val result=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(3),20,EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.Each)),EffectContext(null,player))
        val summaries=result.events.filterIsInstance<DiceRolledEvent>()
        summaries.map { it.playerId }.toSet() shouldBe setOf(player,opponent)
        for (summary in summaries) {
            summary.results shouldBe result.events.filterIsInstance<DieRolledEvent>().filter { it.playerId==summary.playerId }.map { it.result }
            summary.sides shouldBe 20
            // The summary trails its roller's dice, so a batch observer sees the finished roll.
            result.events.indexOf(summary) shouldBe result.events.indexOfLast { it is DieRolledEvent && it.playerId==summary.playerId }+1
        }
        com.wingedsheep.engine.view.ClientEventTransformer.transform(summaries,player) shouldBe emptyList()
        val ctx=EffectContext(null,player,triggerContext=TriggerContext.fromEvent(DiceRolledEvent(player,20,listOf(4,17,9))))
        PredicateEvaluator(d.cardRegistry).conditions.amounts.evaluate(d.state,DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_HIGHEST_DIE_RESULT),ctx) shouldBe 17
        ctx.triggerContext!!.triggeringPlayerId shouldBe player
    }
    test("a one-or-more-dice observer triggers once per roll instruction, for its roller only") {
        val observer=card("Test Dice Batch Observer") {
            typeLine="Enchantment"
            triggeredAbility {
                trigger=Triggers.you.rollsDice()
                effect=Effects.GainLife(DynamicAmount.Fixed(1))
            }
        }
        val roller=card("Test Batch Roller") {
            typeLine="Artifact"
            activatedAbility { cost=Costs.Tap; effect=Effects.RollDice(DynamicAmount.Fixed(5),6) then Effects.RollDice(DynamicAmount.Fixed(2),6) then Effects.RollDice(DynamicAmount.Fixed(0),6) }
        }
        val d=setup(); d.registerCards(listOf(observer,roller));d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player=d.activePlayer!!;val opponent=d.getOpponent(player)
        d.putPermanentOnBattlefield(player,observer.name)
        d.putPermanentOnBattlefield(opponent,observer.name)
        val source=d.putPermanentOnBattlefield(player,roller.name)
        d.submit(ActivateAbility(player,source,roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        d.bothPass()
        // Two instructions rolled dice; the zero-dice roll is not a roll.
        d.state.stack.size shouldBe 2
        d.bothPass(); d.bothPass()
        d.getLifeTotal(player) shouldBe 22
        d.getLifeTotal(opponent) shouldBe 20
    }
    val advantage=card("Test Dice Advantage") {
        typeLine="Enchantment"
        staticAbility { ability=RollAdditionalDice() }
    }
    fun advantageSetup(vararg owners: Int): Pair<GameTestDriver, List<EntityId>> {
        val d=setup(); d.registerCards(listOf(advantage))
        val players=listOf(d.activePlayer!!, d.getOpponent(d.activePlayer!!))
        for (owner in owners) d.putPermanentOnBattlefield(players[owner], advantage.name)
        d.replaceState(d.state.copy(rng=GameRng.seeded(77)))
        return d to players
    }

    test("roll that many dice plus one: the lowest roll is reported ignored and nothing reads it") {
        val (d, players)=advantageSetup(0); val player=players[0]
        val result=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(3),6,resultCounts=mapOf("all" to (1..6).toSet())),EffectContext(null,player))
        val rolls=result.events.filterIsInstance<DieRolledEvent>()
        rolls.size shouldBe 4
        val ignored=rolls.single { it.ignored }
        ignored.result shouldBe rolls.minOf { it.result }
        val kept=rolls.filterNot { it.ignored }.map { it.result }
        result.events.filterIsInstance<DiceRolledEvent>().single().results shouldBe kept
        result.updatedStoredNumbers shouldBe mapOf("all" to 3)
        val dto=com.wingedsheep.engine.view.ClientEventTransformer.transform(listOf(ignored),player).single() as ClientEvent.DieRolled
        dto.ignored shouldBe true
        dto.description.endsWith(", ignored") shouldBe true
    }
    test("instances add: two sources roll a single d20 as three and keep the highest") {
        val (d, players)=advantageSetup(0,0); val player=players[0]
        val result=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(1),20),EffectContext(null,player))
        val rolls=result.events.filterIsInstance<DieRolledEvent>()
        rolls.size shouldBe 3
        rolls.count { it.ignored } shouldBe 2
        result.events.filterIsInstance<DiceRolledEvent>().single().results shouldBe listOf(rolls.maxOf { it.result })
    }
    test("only the source's controller rolls extra dice, and a roll of zero dice stays zero") {
        val (d, players)=advantageSetup(1)
        val mine=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(2),6),EffectContext(null,players[0]))
        mine.events.filterIsInstance<DieRolledEvent>().size shouldBe 2
        val theirs=executor(d).execute(d.state,RollDiceEffect(DynamicAmount.Fixed(0),6),EffectContext(null,players[1]))
        theirs.events shouldBe emptyList()
    }
    test("an ignored roll triggers no die observer") {
        val observer=card("Test Any Die Observer") {
            typeLine="Enchantment"
            triggeredAbility { trigger=Triggers.you.rollsDie(); effect=Effects.GainLife(DynamicAmount.Fixed(1)) }
        }
        val roller=card("Test Single Roller") {
            typeLine="Artifact"
            activatedAbility { cost=Costs.Tap; effect=Effects.RollDice(DynamicAmount.Fixed(1),6) }
        }
        val (d, players)=advantageSetup(0); d.registerCards(listOf(observer,roller)); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player=players[0]
        d.putPermanentOnBattlefield(player,observer.name)
        val source=d.putPermanentOnBattlefield(player,roller.name)
        d.submit(ActivateAbility(player,source,roller.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        val resolved=d.bothPass()
        resolved.events.filterIsInstance<DieRolledEvent>().size shouldBe 2
        d.state.stack.size shouldBe 1
    }
})
