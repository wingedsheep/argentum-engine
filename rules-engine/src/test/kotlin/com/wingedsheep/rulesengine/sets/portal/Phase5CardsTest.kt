package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Phase 5 Portal cards - ETB and Death Triggers.
 */
class Phase5CardsTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCardToBattlefield(
        def: CardDefinition,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        return cardId to state1.addToZone(cardId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addCardToGraveyard(
        def: CardDefinition,
        ownerId: EntityId
    ): Pair<EntityId, GameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, ownerId)
        )
        val (cardId, state1) = createEntity(EntityId.generate(), components)
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, ownerId)
        return cardId to state1.addToZone(cardId, graveyardZone)
    }

    val registry = EffectHandlerRegistry.default()

    // =========================================================================
    // Card Registration Tests
    // =========================================================================

    context("Phase 5 card registration") {
        test("Venerable Monk is registered correctly") {
            val card = PortalSet.getCardDefinition("Venerable Monk")
            card.shouldNotBeNull()
            card.name shouldBe "Venerable Monk"
            card.manaCost.toString() shouldBe "{2}{W}"
            card.isCreature shouldBe true
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
        }

        test("Spiritual Guardian is registered correctly") {
            val card = PortalSet.getCardDefinition("Spiritual Guardian")
            card.shouldNotBeNull()
            card.name shouldBe "Spiritual Guardian"
            card.manaCost.toString() shouldBe "{3}{W}{W}"
            card.creatureStats?.basePower shouldBe 3
            card.creatureStats?.baseToughness shouldBe 4
        }

        test("Dread Reaper is registered correctly") {
            val card = PortalSet.getCardDefinition("Dread Reaper")
            card.shouldNotBeNull()
            card.name shouldBe "Dread Reaper"
            card.manaCost.toString() shouldBe "{3}{B}{B}{B}"
            card.creatureStats?.basePower shouldBe 6
            card.creatureStats?.baseToughness shouldBe 5
            card.keywords shouldContain Keyword.FLYING
        }

        test("Serpent Warrior is registered correctly") {
            val card = PortalSet.getCardDefinition("Serpent Warrior")
            card.shouldNotBeNull()
            card.name shouldBe "Serpent Warrior"
            card.manaCost.toString() shouldBe "{2}{B}"
            card.creatureStats?.basePower shouldBe 3
            card.creatureStats?.baseToughness shouldBe 3
        }

        test("Fire Imp is registered correctly") {
            val card = PortalSet.getCardDefinition("Fire Imp")
            card.shouldNotBeNull()
            card.name shouldBe "Fire Imp"
            card.manaCost.toString() shouldBe "{2}{R}"
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 1
        }

        test("Man-o'-War is registered correctly") {
            val card = PortalSet.getCardDefinition("Man-o'-War")
            card.shouldNotBeNull()
            card.name shouldBe "Man-o'-War"
            card.manaCost.toString() shouldBe "{2}{U}"
            card.creatureStats?.basePower shouldBe 2
            card.creatureStats?.baseToughness shouldBe 2
        }

        test("Owl Familiar is registered correctly") {
            val card = PortalSet.getCardDefinition("Owl Familiar")
            card.shouldNotBeNull()
            card.name shouldBe "Owl Familiar"
            card.manaCost.toString() shouldBe "{1}{U}"
            card.keywords shouldContain Keyword.FLYING
        }

        test("Ebon Dragon is registered correctly") {
            val card = PortalSet.getCardDefinition("Ebon Dragon")
            card.shouldNotBeNull()
            card.name shouldBe "Ebon Dragon"
            card.manaCost.toString() shouldBe "{5}{B}{B}"
            card.keywords shouldContain Keyword.FLYING
        }

        test("Gravedigger is registered correctly") {
            val card = PortalSet.getCardDefinition("Gravedigger")
            card.shouldNotBeNull()
            card.name shouldBe "Gravedigger"
            card.manaCost.toString() shouldBe "{3}{B}"
        }

        test("Endless Cockroaches is registered correctly") {
            val card = PortalSet.getCardDefinition("Endless Cockroaches")
            card.shouldNotBeNull()
            card.name shouldBe "Endless Cockroaches"
            card.manaCost.toString() shouldBe "{1}{B}{B}"
        }

        test("Undying Beast is registered correctly") {
            val card = PortalSet.getCardDefinition("Undying Beast")
            card.shouldNotBeNull()
            card.name shouldBe "Undying Beast"
            card.manaCost.toString() shouldBe "{3}{B}"
        }

        test("Noxious Toad is registered correctly") {
            val card = PortalSet.getCardDefinition("Noxious Toad")
            card.shouldNotBeNull()
            card.name shouldBe "Noxious Toad"
            card.manaCost.toString() shouldBe "{2}{B}"
        }

        test("Fire Snake is registered correctly") {
            val card = PortalSet.getCardDefinition("Fire Snake")
            card.shouldNotBeNull()
            card.name shouldBe "Fire Snake"
            card.manaCost.toString() shouldBe "{4}{R}"
        }

        test("Charging Bandits is registered correctly") {
            val card = PortalSet.getCardDefinition("Charging Bandits")
            card.shouldNotBeNull()
            card.name shouldBe "Charging Bandits"
            card.manaCost.toString() shouldBe "{4}{B}"
        }

        test("Charging Paladin is registered correctly") {
            val card = PortalSet.getCardDefinition("Charging Paladin")
            card.shouldNotBeNull()
            card.name shouldBe "Charging Paladin"
            card.manaCost.toString() shouldBe "{2}{W}"
        }

        test("Seasoned Marshal is registered correctly") {
            val card = PortalSet.getCardDefinition("Seasoned Marshal")
            card.shouldNotBeNull()
            card.name shouldBe "Seasoned Marshal"
            card.manaCost.toString() shouldBe "{2}{W}{W}"
        }

        test("Serpent Assassin is registered correctly") {
            val card = PortalSet.getCardDefinition("Serpent Assassin")
            card.shouldNotBeNull()
            card.name shouldBe "Serpent Assassin"
            card.manaCost.toString() shouldBe "{3}{B}{B}"
        }
    }

    // =========================================================================
    // ETB Trigger Script Tests
    // =========================================================================

    context("ETB trigger scripts") {
        test("Venerable Monk has ETB life gain trigger") {
            val script = PortalSet.getCardScript("Venerable Monk")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
            trigger.effect.shouldBeInstanceOf<GainLifeEffect>()
            (trigger.effect as GainLifeEffect).amount shouldBe 2
        }

        test("Dread Reaper has ETB life loss trigger") {
            val script = PortalSet.getCardScript("Dread Reaper")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<LoseLifeEffect>()
            (trigger.effect as LoseLifeEffect).amount shouldBe 5
        }

        test("Fire Imp has ETB damage trigger") {
            val script = PortalSet.getCardScript("Fire Imp")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<DealDamageEffect>()
            (trigger.effect as DealDamageEffect).amount shouldBe 2
        }

        test("Man-o'-War has ETB bounce trigger") {
            val script = PortalSet.getCardScript("Man-o'-War")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<ReturnToHandEffect>()
        }

        test("Owl Familiar has ETB draw and discard trigger") {
            val script = PortalSet.getCardScript("Owl Familiar")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<CompositeEffect>()
            val composite = trigger.effect as CompositeEffect
            composite.effects shouldHaveSize 2
            composite.effects[0].shouldBeInstanceOf<DrawCardsEffect>()
            composite.effects[1].shouldBeInstanceOf<DiscardCardsEffect>()
        }
    }

    // =========================================================================
    // Death Trigger Script Tests
    // =========================================================================

    context("Death trigger scripts") {
        test("Endless Cockroaches has death return to hand trigger") {
            val script = PortalSet.getCardScript("Endless Cockroaches")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnDeath>()
            trigger.effect.shouldBeInstanceOf<ReturnToHandEffect>()
        }

        test("Undying Beast has death put on top of library trigger") {
            val script = PortalSet.getCardScript("Undying Beast")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnDeath>()
            trigger.effect.shouldBeInstanceOf<PutOnTopOfLibraryEffect>()
        }

        test("Noxious Toad has death each opponent discards trigger") {
            val script = PortalSet.getCardScript("Noxious Toad")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<DiscardCardsEffect>()
            (trigger.effect as DiscardCardsEffect).target shouldBe EffectTarget.EachOpponent
        }

        test("Fire Snake has death destroy land trigger") {
            val script = PortalSet.getCardScript("Fire Snake")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.effect.shouldBeInstanceOf<DestroyEffect>()
            (trigger.effect as DestroyEffect).target shouldBe EffectTarget.TargetLand
        }
    }

    // =========================================================================
    // Attack Trigger Script Tests
    // =========================================================================

    context("Attack trigger scripts") {
        test("Charging Bandits has attack power boost trigger") {
            val script = PortalSet.getCardScript("Charging Bandits")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnAttack>()
            trigger.effect.shouldBeInstanceOf<ModifyStatsEffect>()
            val effect = trigger.effect as ModifyStatsEffect
            effect.powerModifier shouldBe 2
            effect.toughnessModifier shouldBe 0
        }

        test("Charging Paladin has attack toughness boost trigger") {
            val script = PortalSet.getCardScript("Charging Paladin")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnAttack>()
            val effect = trigger.effect as ModifyStatsEffect
            effect.powerModifier shouldBe 0
            effect.toughnessModifier shouldBe 3
        }

        test("Seasoned Marshal has attack tap trigger") {
            val script = PortalSet.getCardScript("Seasoned Marshal")
            script.shouldNotBeNull()
            script.triggeredAbilities shouldHaveSize 1

            val trigger = script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnAttack>()
            trigger.effect.shouldBeInstanceOf<TapUntapEffect>()
        }
    }

    // =========================================================================
    // Effect Execution Tests
    // =========================================================================

    context("GainLifeEffect execution") {
        test("gains life for controller") {
            var state = newGame()

            val effect = GainLifeEffect(4, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val initialLife = state.getLife(player1Id)
            val result = registry.execute(state, effect, context)

            result.state.getLife(player1Id) shouldBe initialLife + 4
        }
    }

    context("LoseLifeEffect execution") {
        test("loses life for controller") {
            var state = newGame()

            val effect = LoseLifeEffect(3, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val initialLife = state.getLife(player1Id)
            val result = registry.execute(state, effect, context)

            result.state.getLife(player1Id) shouldBe initialLife - 3
        }
    }

    context("PutOnTopOfLibraryEffect execution") {
        test("puts card on top of library") {
            var state = newGame()
            val creatureDef = CardDefinition.creature(
                name = "Test Creature",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.BEAST),
                power = 1,
                toughness = 1
            )

            // Put creature in graveyard (simulating death)
            val (creatureId, state1) = state.addCardToGraveyard(creatureDef, player1Id)
            state = state1

            val libraryZone = ZoneId(ZoneType.LIBRARY, player1Id)
            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            val effect = PutOnTopOfLibraryEffect(EffectTarget.Self)
            val context = ExecutionContext(player1Id, creatureId)

            val result = registry.execute(state, effect, context)

            // Should be on top of library (index 0), not in graveyard
            result.state.getZone(graveyardZone) shouldNotContain creatureId
            result.state.getZone(libraryZone).firstOrNull() shouldBe creatureId
        }
    }

    context("ReturnFromGraveyardEffect execution") {
        test("returns creature from graveyard to hand") {
            var state = newGame()
            val creatureDef = CardDefinition.creature(
                name = "Test Creature",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.ZOMBIE),
                power = 2,
                toughness = 2
            )

            val (creatureId, state1) = state.addCardToGraveyard(creatureDef, player1Id)
            state = state1

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val graveyardZone = ZoneId(ZoneType.GRAVEYARD, player1Id)

            val effect = ReturnFromGraveyardEffect(
                filter = CardFilter.CreatureCard,
                destination = SearchDestination.HAND
            )
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            result.state.getZone(graveyardZone) shouldNotContain creatureId
            result.state.getZone(handZone) shouldContain creatureId
        }

        test("does nothing if no matching card in graveyard") {
            var state = newGame()

            val handZone = ZoneId(ZoneType.HAND, player1Id)
            val initialHandSize = state.getZone(handZone).size

            val effect = ReturnFromGraveyardEffect(
                filter = CardFilter.CreatureCard,
                destination = SearchDestination.HAND
            )
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Hand should be unchanged
            result.state.getZone(handZone).size shouldBe initialHandSize
        }
    }
})
