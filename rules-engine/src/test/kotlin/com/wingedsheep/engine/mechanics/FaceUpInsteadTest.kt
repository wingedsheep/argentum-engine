package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TurnsFaceUpInsteadComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/**
 * Illusionary Mask's rider ([FaceUpInstead]): a face-down permanent that "has not been turned face
 * up and would assign or deal damage, be dealt damage, or become tapped" is turned face up instead,
 * and then the event happens to — or is done by — the face-up permanent.
 */
class FaceUpInsteadTest : FunSpec({

    val tapPulse = card("Face Up Test Tap Pulse") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Tap(t)
        }
    }
    val anthemBeast = card("Face Up Test Anthem Beast") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        staticAbility { ability = ModifyStats(1, 1, GroupFilter.OtherCreaturesYouControl) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(tapPulse, anthemBeast))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Put [name] onto the battlefield face down, carrying the rider the way Illusionary Mask leaves it. */
    fun masked(d: GameTestDriver, player: EntityId, name: String, rider: Boolean = true): EntityId {
        val id = d.putCreatureOnBattlefield(player, name)
        d.removeSummoningSickness(id)
        val handler = StaticAbilityHandler(d.cardRegistry)
        d.replaceState(d.state.updateEntity(id) { c ->
            val stamped = if (rider) FaceUpInstead.stamp(c, handler) else c
            stamped.without<ContinuousEffectSourceComponent>()
                .without<ReplacementEffectSourceComponent>()
                .with(FaceDownComponent)
        })
        return id
    }

    fun GameTestDriver.faceDown(id: EntityId) = state.getEntity(id)!!.has<FaceDownComponent>()
    fun GameTestDriver.hasRider(id: EntityId) = state.getEntity(id)!!.has<TurnsFaceUpInsteadComponent>()
    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    // ---------------------------------------------------------------- becomes tapped

    test("the tap atom turns it face up first and then taps it") {
        val d = driver()
        val id = masked(d, d.player1, "Morph Trigger Test Creature")

        val outcome = tap(d.state, id)

        outcome.events.map { it::class } shouldBe listOf(TurnFaceUpEvent::class, TappedEvent::class)
        outcome.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe false
        outcome.state.getEntity(id)!!.has<TurnsFaceUpInsteadComponent>() shouldBe false
        outcome.state.projectedState.getPower(id) shouldBe 4
        outcome.tapped!!.entityName shouldBe "Morph Trigger Test Creature"
    }

    test("a tap effect turns it face up and its turned-face-up trigger fires") {
        val d = driver()
        val id = masked(d, d.player1, "Morph Trigger Test Creature")
        val handBefore = d.getHandSize(d.player1)
        val pulse = d.putCardInHand(d.player1, tapPulse.name)

        d.castSpell(d.player1, pulse, listOf(id)).error shouldBe null
        d.bothPass()
        // The "when this is turned face up, draw a card" trigger goes on the stack.
        d.bothPass()

        d.faceDown(id) shouldBe false
        d.isTapped(id) shouldBe true
        d.getHandSize(d.player1) shouldBe handBefore + 1
    }

    test("an already tapped face-down permanent can't become tapped, so it stays face down (CR 701.26a)") {
        val d = driver()
        val id = masked(d, d.player1, "Centaur Courser")
        d.tapPermanent(id)

        val outcome = tap(d.state, id)

        outcome.events shouldBe emptyList()
        outcome.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe true
    }

    test("a face-down permanent without the rider just becomes tapped") {
        val d = driver()
        val id = masked(d, d.player1, "Centaur Courser", rider = false)

        val outcome = tap(d.state, id)

        outcome.events.map { it::class } shouldBe listOf(TappedEvent::class)
        outcome.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe true
    }

    test("tapping to attack turns it face up and it deals its real power") {
        val d = driver()
        val id = masked(d, d.player1, "Centaur Courser")
        val opponentLife = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(id), d.player2).error shouldBe null
        d.faceDown(id) shouldBe false
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.END_COMBAT)

        d.getLifeTotal(d.player2) shouldBe opponentLife - 3
    }

    test("turning face up installs the card's static abilities (CR 708.8)") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val id = masked(d, d.player1, anthemBeast.name)
        d.state.projectedState.getPower(bears) shouldBe 2

        d.replaceState(tap(d.state, id).state)

        d.state.projectedState.getPower(bears) shouldBe 3
        d.state.projectedState.getToughness(bears) shouldBe 3
    }

    test("the rider ends once it has been turned face up, and leaving the battlefield drops it") {
        val d = driver()
        val id = masked(d, d.player1, "Centaur Courser")
        d.hasRider(id) shouldBe true

        d.replaceState(tap(d.state, id).state)
        d.hasRider(id) shouldBe false

        val other = masked(d, d.player1, "Grizzly Bears")
        val stripped = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
            .stripBattlefieldComponents(d.state.getEntity(other)!!)
        stripped.has<TurnsFaceUpInsteadComponent>() shouldBe false
    }

    // ---------------------------------------------------------------- non-combat damage

    test("dealt non-combat damage, it turns face up first and the damage is dealt to the face-up creature") {
        val d = driver()
        val id = masked(d, d.player2, "Morph Trigger Test Creature")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, com.wingedsheep.sdk.core.Color.RED, 1)

        d.castSpell(d.player1, bolt, listOf(id)).error shouldBe null
        d.bothPass()

        // Face down it was a 2/2 that 3 damage kills; face up it is a 4/4 that survives it.
        d.faceDown(id) shouldBe false
        (id in d.state.getBattlefield()) shouldBe true
        d.damageOn(id) shouldBe 3
    }

    test("dealt 2 non-combat damage, a face-up 3/3 survives what would have killed a 2/2") {
        val d = driver()
        val id = masked(d, d.player2, "Centaur Courser")

        val result = DamageUtils.dealDamageToTarget(d.zones, d.state, id, 2, sourceId = null)

        result.events.first()::class shouldBe TurnFaceUpEvent::class
        result.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe false
        result.state.getEntity(id)?.get<DamageComponent>()?.amount shouldBe 2
        result.state.projectedState.getToughness(id) shouldBe 3
    }

    test("dealing non-combat damage, it turns face up first and its real deathtouch applies") {
        val d = driver()
        val rat = masked(d, d.player1, "Deathtouch Rat")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        val result = DamageUtils.dealDamageToTarget(d.zones, d.state, courser, 1, sourceId = rat)

        result.state.getEntity(rat)!!.has<FaceDownComponent>() shouldBe false
        result.events.first()::class shouldBe TurnFaceUpEvent::class
        // The face-up Rat has deathtouch, so its 1 damage is lethal (CR 702.2b).
        result.state.getEntity(courser)!!.get<DamageComponent>()!!.deathtouchDamageReceived shouldBe true
    }

    test("zero damage is no damage, so it stays face down") {
        val d = driver()
        val id = masked(d, d.player2, "Centaur Courser")

        val result = DamageUtils.dealDamageToTarget(d.zones, d.state, id, 0, sourceId = null)

        result.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe true
    }

    // ---------------------------------------------------------------- combat damage

    test("a face-down blocker turns face up as it would assign combat damage and survives as its real self") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(attacker)
        val blocker = masked(d, d.player2, "Morph Trigger Test Creature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)

        // Face up it is a 4/4: it kills the 3/3 Courser and survives its 3 damage.
        d.faceDown(blocker) shouldBe false
        (blocker in d.state.getBattlefield()) shouldBe true
        (attacker in d.state.getBattlefield()) shouldBe false
        d.damageOn(blocker) shouldBe 3
    }

    test("a face-down blocker that assigns no damage but is dealt some turns face up as it is dealt it") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(attacker)
        val blocker = masked(d, d.player2, "Morph Trigger Test Creature")
        // -4/-0 makes the face-down 2/2 a -2/2, which assigns no combat damage (CR 510.1a).
        d.replaceState(d.state.addFloatingEffect(
            layer = com.wingedsheep.engine.mechanics.layers.Layer.POWER_TOUGHNESS,
            sublayer = com.wingedsheep.engine.mechanics.layers.Sublayer.MODIFICATIONS,
            modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.ModifyPowerToughness(-4, 0),
            affectedEntities = setOf(blocker),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = com.wingedsheep.engine.handlers.EffectContext(sourceId = blocker, controllerId = d.player2),
        ))

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)

        // It assigned nothing (0 power face up too), but the Courser's 3 damage turned it face up
        // before it was dealt, so the 0/4 survives instead of dying as a -2/2.
        d.faceDown(blocker) shouldBe false
        (blocker in d.state.getBattlefield()) shouldBe true
        (attacker in d.state.getBattlefield()) shouldBe true
    }

    test("a face-down blocker with nothing left to block assigns no damage and stays face down (CR 510.1c)") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(attacker)
        val blocker = masked(d, d.player2, "Morph Trigger Test Creature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).error shouldBe null
        d.moveToGraveyard(attacker)
        d.passPriorityUntil(Step.END_COMBAT)

        d.faceDown(blocker) shouldBe true
    }

    test("a face-down blocker that turns face up into a first striker still assigns in the only damage step (CR 702.7c)") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(attacker)
        val blocker = masked(d, d.player2, "First Strike Knight")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(blocker to listOf(attacker))).error shouldBe null
        d.passPriorityUntil(Step.END_COMBAT)

        // No first-strike step: nobody had first strike as combat damage began. The Knight turns
        // face up as it would assign, gains first strike, and still deals its 3 simultaneously.
        d.faceDown(blocker) shouldBe false
        (attacker in d.state.getBattlefield()) shouldBe false
        (blocker in d.state.getBattlefield()) shouldBe false
    }

    test("a first striker that loses first strike after the first-strike step doesn't assign again (CR 702.7c)") {
        val d = driver()
        val knight = d.putCreatureOnBattlefield(d.player1, "First Strike Knight")
        d.removeSummoningSickness(knight)
        val opponentLife = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(knight), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.FIRST_STRIKE_COMBAT_DAMAGE)
        d.getLifeTotal(d.player2) shouldBe opponentLife - 3
        d.replaceState(d.state.addFloatingEffect(
            layer = com.wingedsheep.engine.mechanics.layers.Layer.ABILITY,
            modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.RemoveKeyword(Keyword.FIRST_STRIKE.name),
            affectedEntities = setOf(knight),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = com.wingedsheep.engine.handlers.EffectContext(sourceId = knight, controllerId = d.player1),
        ))
        d.passPriorityUntil(Step.END_COMBAT)

        d.getLifeTotal(d.player2) shouldBe opponentLife - 3
    }

    test("a creature that gains first strike after the first-strike step still assigns in the regular step (CR 702.7c)") {
        val d = driver()
        val knight = d.putCreatureOnBattlefield(d.player1, "First Strike Knight")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(knight)
        d.removeSummoningSickness(bears)
        val opponentLife = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(knight, bears), d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2)
        d.passPriorityUntil(Step.FIRST_STRIKE_COMBAT_DAMAGE)
        d.getLifeTotal(d.player2) shouldBe opponentLife - 3
        d.replaceState(d.state.addFloatingEffect(
            layer = com.wingedsheep.engine.mechanics.layers.Layer.ABILITY,
            modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.GrantKeyword(Keyword.FIRST_STRIKE.name),
            affectedEntities = setOf(bears),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = com.wingedsheep.engine.handlers.EffectContext(sourceId = bears, controllerId = d.player1),
        ))
        d.passPriorityUntil(Step.END_COMBAT)

        d.getLifeTotal(d.player2) shouldBe opponentLife - 5
    }

    // ---------------------------------------------------------------- serialization

    test("the rider and its baked face-up statics survive a serialization round trip") {
        val d = driver()
        val id = masked(d, d.player1, anthemBeast.name)
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }

        val restored = json.decodeFromString<GameState>(json.encodeToString(d.state))

        val rider = restored.getEntity(id)!!.get<TurnsFaceUpInsteadComponent>()!!
        rider shouldBe d.state.getEntity(id)!!.get<TurnsFaceUpInsteadComponent>()
        (rider.faceUpStatics != null) shouldBe true
    }
})
