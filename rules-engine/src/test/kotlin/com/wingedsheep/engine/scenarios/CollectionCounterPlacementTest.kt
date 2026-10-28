package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.*
import com.wingedsheep.engine.handlers.effects.permanent.counters.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Generic placement invariants; no production card definitions are introduced here. */
class CollectionCounterPlacementTest : FunSpec({
    val ordinary=card("Collection Ordinary") {typeLine="Creature";power=4;toughness=4}
    val prohibited=card("Collection Prohibited") {typeLine="Creature";power=4;toughness=4
        staticAbility {ability=CantReceiveCounters(GroupFilter.source())}}
    fun modifier(n: Int)=card("Collection Modifier $n") {typeLine="Enchantment";replacementEffect(ModifyCounterPlacement(modifier=n))}
    val capped=card("Collection Capped") {typeLine="Creature";power=4;toughness=4
        staticAbility {ability=MaxCounters(CounterType.CHARGE,3)}}
    val playerCap=card("Collection Player Cap") {typeLine="Creature";power=4;toughness=4
        replacementEffect(CapCounterPlacementThisTurn())}
    fun driver()=GameTestDriver().also {d ->
        d.registerCards(TestCards.all+listOf(ordinary,prohibited,capped,playerCap,modifier(1),modifier(-1)))
        d.initMirrorMatch(deck=Deck.of("Forest" to 40));d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun counters(d: GameTestDriver,id: EntityId,kind: CounterType)=d.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0
    fun context(d: GameTestDriver,ids: List<EntityId>)=EffectContext(sourceId=null,controllerId=d.player1,pipeline=PipelineState(storedCollections=mapOf("recipients" to ids)))
    fun place(d: GameTestDriver,ids: List<EntityId>,kind: CounterType=CounterType.PLUS_ONE_PLUS_ONE,n: Int=1): EffectResult {
        val result=AddCountersToCollectionExecutor(d.services.dynamicAmountEvaluator).execute(d.state,
            AddCountersToCollectionEffect("recipients",kind,n),context(d,ids))
        result.error shouldBe null;d.replaceState(result.state);return result
    }
    fun spell(d: GameTestDriver,kind: CounterType,n: Int=1,direct: EntityId?=null) {
        val s=card("Collection Spell") {manaCost="{0}";typeLine="Sorcery";spell {
            effect=(if(direct==null) Effects.Pipeline {
                val all=gather(GameObjectFilter.Creature)
                run(Effects.AddCountersToCollection(all,kind,n))
            } else Effects.AddCounters(kind,n,EffectTarget.SpecificEntity(direct))) then Effects.GainLife(1)
        }}
        d.registerCard(s);val id=d.putCardInHand(d.player1,s.name);d.castSpell(d.player1,id).error shouldBe null
        repeat(40) {if(d.pendingDecision!=null) error("Unexpected placement decision") else if(d.stackSize>0) d.bothPass().error shouldBe null else return}
        error("Unsettled collection spell")
    }
    for(kind in listOf(CounterType.PLUS_ONE_PLUS_ONE,CounterType.MINUS_ONE_MINUS_ONE,CounterType.CHARGE)) {
        test("mixed recipients respect prohibition and continue for $kind") {
            val d=driver();val no=d.putCreatureOnBattlefield(d.player1,prohibited.name)
            val yes=d.putCreatureOnBattlefield(d.player2,ordinary.name)
            d.state.projectedState.canReceiveCounters(no) shouldBe false
            spell(d,kind,2)
            counters(d,yes,kind) shouldBe 2;counters(d,no,kind) shouldBe 0;d.getLifeTotal(d.player1) shouldBe 21
            val events=d.events.filterIsInstance<CountersAddedEvent>();events.size shouldBe 1
            events.single().entityId shouldBe yes;events.single().placedBy shouldBe d.player1
            events.single().amount shouldBe 2;events.single().counterType shouldBe kind
            events.single().entityName shouldBe ordinary.name
            events.single().firstThisTurn shouldBe true;events.single().firstOfTypeThisTurn shouldBe true
        }
    }
    test("all prohibited recipients emit no placement events and later instruction resolves") {
        val d=driver();val ids=List(2) {d.putCreatureOnBattlefield(d.player1,prohibited.name)}
        spell(d,CounterType.PLUS_ONE_PLUS_ONE)
        ids.forEach {counters(d,it,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0}
        d.events.filterIsInstance<CountersAddedEvent>() shouldBe emptyList();d.getLifeTotal(d.player1) shouldBe 21
    }
    test("multiple ordinary recipients receive correct counters and preserve unrelated kinds") {
        val d=driver();val ids=List(3) {d.putCreatureOnBattlefield(d.player1,ordinary.name)}
        d.replaceState(d.state.updateEntity(ids.first()) {it.with(CountersComponent(mapOf(CounterType.CHARGE to 1)))})
        spell(d,CounterType.PLUS_ONE_PLUS_ONE,2)
        ids.forEach {counters(d,it,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2}
        counters(d,ids.first(),CounterType.CHARGE) shouldBe 1
        d.events.filterIsInstance<CountersAddedEvent>().map {it.entityId}.toSet() shouldBe ids.toSet()
    }
    for(blocked in listOf(false,true)) test("single-object and collection state and events agree: prohibited=$blocked") {
        fun run(direct: Boolean): Pair<Int,List<Pair<CounterType,Int>>> {
            val d=driver();val id=d.putCreatureOnBattlefield(d.player1,if(blocked) prohibited.name else ordinary.name)
            spell(d,CounterType.PLUS_ONE_PLUS_ONE,2,if(direct) id else null)
            return counters(d,id,CounterType.PLUS_ONE_PLUS_ONE) to d.events.filterIsInstance<CountersAddedEvent>().map {it.counterType to it.amount}
        }
        run(false) shouldBe run(true)
    }
    test("additive replacement remains recipient and controller scoped after prohibition") {
        val d=driver();val no=d.putCreatureOnBattlefield(d.player1,prohibited.name)
        val own=d.putCreatureOnBattlefield(d.player1,ordinary.name);val other=d.putCreatureOnBattlefield(d.player2,ordinary.name)
        d.putPermanentOnBattlefield(d.player1,modifier(1).name);spell(d,CounterType.PLUS_ONE_PLUS_ONE)
        counters(d,no,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        counters(d,own,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2;counters(d,other,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
    test("doubling replacement does not override a prohibition") {
        val d=driver();val no=d.putCreatureOnBattlefield(d.player1,prohibited.name)
        val yes=d.putCreatureOnBattlefield(d.player1,ordinary.name)
        val doubling=card("Collection Doubler") {typeLine="Enchantment";replacementEffect(ScaleCounterPlacement())}
        d.registerCard(doubling);d.putPermanentOnBattlefield(d.player1,doubling.name)
        spell(d,CounterType.PLUS_ONE_PLUS_ONE,2)
        counters(d,yes,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4;counters(d,no,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }
    test("replacement reducing placement to zero emits no event or first-placement mark") {
        val d=driver();val id=d.putCreatureOnBattlefield(d.player1,ordinary.name)
        val source=d.putPermanentOnBattlefield(d.player1,modifier(-1).name)
        place(d,listOf(id)).events shouldBe emptyList();counters(d,id,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.moveToGraveyard(source)
        val event=place(d,listOf(id)).events.filterIsInstance<CountersAddedEvent>().single()
        event.firstThisTurn shouldBe true;event.firstOfTypeThisTurn shouldBe true
    }
    test("at maximum counters collection placement is a no-op without an event") {
        val d=driver();val id=d.putCreatureOnBattlefield(d.player1,capped.name)
        d.replaceState(d.state.updateEntity(id) {it.with(CountersComponent(mapOf(CounterType.CHARGE to 3)))})
        place(d,listOf(id),CounterType.CHARGE,2).events shouldBe emptyList()
        counters(d,id,CounterType.CHARGE) shouldBe 3
    }
    test("player placement records cap lock even when its source leaves") {
        val d=driver();val source=d.putCreatureOnBattlefield(d.player1,playerCap.name)
        val first=place(d,listOf(d.player1),CounterType.POISON,3)
        counters(d,d.player1,CounterType.POISON) shouldBe 1
        first.events.filterIsInstance<CountersAddedEvent>().single().amount shouldBe 1
        d.moveToGraveyard(source)
        place(d,listOf(d.player1),CounterType.POISON,3).events shouldBe emptyList()
        counters(d,d.player1,CounterType.POISON) shouldBe 1
    }
    test("zero and negative amounts do not create events even with an additive replacement") {
        val d=driver();val id=d.putCreatureOnBattlefield(d.player1,ordinary.name)
        d.putPermanentOnBattlefield(d.player1,modifier(1).name)
        for(n in listOf(0,-1)) {place(d,listOf(id),n=n).events shouldBe emptyList();counters(d,id,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0}
    }
    test("empty and nonexistent collection members are skipped without stopping eligible recipients") {
        val d=driver();val id=d.putCreatureOnBattlefield(d.player1,ordinary.name)
        place(d,emptyList()).events shouldBe emptyList()
        val events=place(d,listOf(EntityId("absent"),id)).events.filterIsInstance<CountersAddedEvent>()
        events.map {it.entityId} shouldBe listOf(id);counters(d,id,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
    test("dynamic amount overrides count and is evaluated once before any recipient changes") {
        val d=driver();val ids=List(2) {d.putCreatureOnBattlefield(d.player1,ordinary.name)}
        val result=AddCountersToCollectionExecutor(d.services.dynamicAmountEvaluator).execute(d.state,
            AddCountersToCollectionEffect("recipients",CounterType.PLUS_ONE_PLUS_ONE,99,
                amount=DynamicAmounts.powerOf(EffectTarget.SpecificEntity(ids.first()))),context(d,ids))
        result.error shouldBe null;d.replaceState(result.state)
        ids.forEach {counters(d,it,CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4}
    }
    test("first-placement flags are tracked separately for recipient and counter kind") {
        val d=driver();val id=d.putCreatureOnBattlefield(d.player1,ordinary.name)
        val first=place(d,listOf(id)).events.filterIsInstance<CountersAddedEvent>().single()
        val repeat=place(d,listOf(id)).events.filterIsInstance<CountersAddedEvent>().single()
        val another=place(d,listOf(id),CounterType.CHARGE).events.filterIsInstance<CountersAddedEvent>().single()
        first.firstThisTurn shouldBe true;first.firstOfTypeThisTurn shouldBe true
        repeat.firstThisTurn shouldBe false;repeat.firstOfTypeThisTurn shouldBe false
        another.firstThisTurn shouldBe false;another.firstOfTypeThisTurn shouldBe true
    }
    test("state-based deaths happen after the following instruction, not between collection recipients") {
        val d=driver();val ids=List(2) {d.putCreatureOnBattlefield(d.player1,ordinary.name)}
        val start=d.events.size
        spell(d,CounterType.MINUS_ONE_MINUS_ONE,4)
        d.getGraveyard(d.player1).containsAll(ids) shouldBe true;d.getLifeTotal(d.player1) shouldBe 21
        val life=d.events.drop(start).indexOfFirst {it is LifeChangedEvent}
        val death=d.events.drop(start).indexOfFirst {it is ZoneChangeEvent && it.entityId in ids && it.toZone==Zone.GRAVEYARD}
        (life>=0 && death>life) shouldBe true
    }
})
