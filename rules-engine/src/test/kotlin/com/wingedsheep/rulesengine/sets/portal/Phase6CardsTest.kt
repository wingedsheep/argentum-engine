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
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.script.ResolvedTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.handler.EffectHandlerRegistry
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Phase 6 Portal cards - Damage and Drain Spells.
 */
class Phase6CardsTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.addCreatureToBattlefield(
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

    val registry = EffectHandlerRegistry.default()

    val flyingCreatureDef = CardDefinition.creature(
        name = "Flying Test",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.BIRD),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING)
    )

    val groundCreatureDef = CardDefinition.creature(
        name = "Ground Test",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    // =========================================================================
    // Card Registration Tests
    // =========================================================================

    context("Phase 6 card registration") {
        test("Scorching Spear is registered correctly") {
            val card = PortalSet.getCardDefinition("Scorching Spear")
            card.shouldNotBeNull()
            card.name shouldBe "Scorching Spear"
            card.manaCost.toString() shouldBe "{R}"
            card.isSorcery shouldBe true
        }

        test("Volcanic Hammer is registered correctly") {
            val card = PortalSet.getCardDefinition("Volcanic Hammer")
            card.shouldNotBeNull()
            card.name shouldBe "Volcanic Hammer"
            card.manaCost.toString() shouldBe "{1}{R}"
        }

        test("Lava Axe is registered correctly") {
            val card = PortalSet.getCardDefinition("Lava Axe")
            card.shouldNotBeNull()
            card.name shouldBe "Lava Axe"
            card.manaCost.toString() shouldBe "{4}{R}"
        }

        test("Bee Sting is registered correctly") {
            val card = PortalSet.getCardDefinition("Bee Sting")
            card.shouldNotBeNull()
            card.name shouldBe "Bee Sting"
            card.manaCost.toString() shouldBe "{3}{G}"
        }

        test("Pyroclasm is registered correctly") {
            val card = PortalSet.getCardDefinition("Pyroclasm")
            card.shouldNotBeNull()
            card.name shouldBe "Pyroclasm"
            card.manaCost.toString() shouldBe "{1}{R}"
        }

        test("Dry Spell is registered correctly") {
            val card = PortalSet.getCardDefinition("Dry Spell")
            card.shouldNotBeNull()
            card.name shouldBe "Dry Spell"
            card.manaCost.toString() shouldBe "{1}{B}"
        }

        test("Fire Tempest is registered correctly") {
            val card = PortalSet.getCardDefinition("Fire Tempest")
            card.shouldNotBeNull()
            card.name shouldBe "Fire Tempest"
            card.manaCost.toString() shouldBe "{5}{R}{R}"
        }

        test("Needle Storm is registered correctly") {
            val card = PortalSet.getCardDefinition("Needle Storm")
            card.shouldNotBeNull()
            card.name shouldBe "Needle Storm"
            card.manaCost.toString() shouldBe "{2}{G}"
        }

        test("Vampiric Feast is registered correctly") {
            val card = PortalSet.getCardDefinition("Vampiric Feast")
            card.shouldNotBeNull()
            card.name shouldBe "Vampiric Feast"
            card.manaCost.toString() shouldBe "{5}{B}"
        }

        test("Vampiric Touch is registered correctly") {
            val card = PortalSet.getCardDefinition("Vampiric Touch")
            card.shouldNotBeNull()
            card.name shouldBe "Vampiric Touch"
            card.manaCost.toString() shouldBe "{2}{B}"
        }

        test("Soul Shred is registered correctly") {
            val card = PortalSet.getCardDefinition("Soul Shred")
            card.shouldNotBeNull()
            card.name shouldBe "Soul Shred"
            card.manaCost.toString() shouldBe "{2}{B}"
        }

        test("Sacred Nectar is registered correctly") {
            val card = PortalSet.getCardDefinition("Sacred Nectar")
            card.shouldNotBeNull()
            card.name shouldBe "Sacred Nectar"
            card.manaCost.toString() shouldBe "{1}{W}"
        }

        test("Natural Spring is registered correctly") {
            val card = PortalSet.getCardDefinition("Natural Spring")
            card.shouldNotBeNull()
            card.name shouldBe "Natural Spring"
            card.manaCost.toString() shouldBe "{3}{G}{G}"
        }
    }

    // =========================================================================
    // Script Tests
    // =========================================================================

    context("Damage spell scripts") {
        test("Scorching Spear has deal damage effect") {
            val script = PortalSet.getCardScript("Scorching Spear")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()
            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<DealDamageEffect>()
            (effect as DealDamageEffect).amount shouldBe 1
            effect.target shouldBe EffectTarget.AnyTarget
        }

        test("Pyroclasm has mass damage effect") {
            val script = PortalSet.getCardScript("Pyroclasm")
            script.shouldNotBeNull()
            script.spellEffect.shouldNotBeNull()
            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<DealDamageToAllCreaturesEffect>()
            (effect as DealDamageToAllCreaturesEffect).amount shouldBe 2
        }

        test("Needle Storm targets only flying creatures") {
            val script = PortalSet.getCardScript("Needle Storm")
            script.shouldNotBeNull()
            val effect = script.spellEffect!!.effect as DealDamageToAllCreaturesEffect
            effect.amount shouldBe 4
            effect.onlyFlying shouldBe true
        }

        test("Dry Spell has damage to all effect") {
            val script = PortalSet.getCardScript("Dry Spell")
            script.shouldNotBeNull()
            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<DealDamageToAllEffect>()
            (effect as DealDamageToAllEffect).amount shouldBe 1
        }
    }

    context("Drain spell scripts") {
        test("Vampiric Feast has drain effect") {
            val script = PortalSet.getCardScript("Vampiric Feast")
            script.shouldNotBeNull()
            val effect = script.spellEffect!!.effect
            effect.shouldBeInstanceOf<DrainEffect>()
            (effect as DrainEffect).amount shouldBe 4
        }

        test("Vampiric Touch has drain effect targeting opponent") {
            val script = PortalSet.getCardScript("Vampiric Touch")
            script.shouldNotBeNull()
            val effect = script.spellEffect!!.effect as DrainEffect
            effect.amount shouldBe 2
            effect.target shouldBe EffectTarget.Opponent
        }

        test("Soul Shred has drain effect targeting nonblack creature") {
            val script = PortalSet.getCardScript("Soul Shred")
            script.shouldNotBeNull()
            val effect = script.spellEffect!!.effect as DrainEffect
            effect.amount shouldBe 3
            effect.target shouldBe EffectTarget.TargetNonblackCreature
        }
    }

    // =========================================================================
    // Effect Execution Tests
    // =========================================================================

    context("DealDamageToAllCreaturesEffect execution") {
        test("deals damage to all creatures") {
            var state = newGame()

            // Add two creatures
            val (creature1Id, state1) = state.addCreatureToBattlefield(groundCreatureDef, player1Id)
            val (creature2Id, state2) = state1.addCreatureToBattlefield(groundCreatureDef, player2Id)
            state = state2

            val effect = DealDamageToAllCreaturesEffect(2)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Both creatures should have 2 damage
            result.state.getEntity(creature1Id)?.get<DamageComponent>()?.amount shouldBe 2
            result.state.getEntity(creature2Id)?.get<DamageComponent>()?.amount shouldBe 2
        }

        test("Needle Storm only damages flying creatures") {
            var state = newGame()

            // Add flying and non-flying creatures
            val (flyingId, state1) = state.addCreatureToBattlefield(flyingCreatureDef, player1Id)
            val (groundId, state2) = state1.addCreatureToBattlefield(groundCreatureDef, player2Id)
            state = state2

            val effect = DealDamageToAllCreaturesEffect(4, onlyFlying = true)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Only flying creature should have damage
            result.state.getEntity(flyingId)?.get<DamageComponent>()?.amount shouldBe 4
            result.state.getEntity(groundId)?.get<DamageComponent>()?.amount shouldBe null
        }
    }

    context("DealDamageToAllEffect execution") {
        test("deals damage to all creatures and all players") {
            var state = newGame()

            val (creatureId, state1) = state.addCreatureToBattlefield(groundCreatureDef, player1Id)
            state = state1

            val initialLife1 = state.getLife(player1Id)
            val initialLife2 = state.getLife(player2Id)

            val effect = DealDamageToAllEffect(3)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Creature should have 3 damage
            result.state.getEntity(creatureId)?.get<DamageComponent>()?.amount shouldBe 3
            // Both players should lose 3 life
            result.state.getLife(player1Id) shouldBe initialLife1 - 3
            result.state.getLife(player2Id) shouldBe initialLife2 - 3
        }
    }

    context("DrainEffect execution") {
        test("deals damage to opponent and gains life") {
            var state = newGame()

            val initialControllerLife = state.getLife(player1Id)
            val initialOpponentLife = state.getLife(player2Id)

            val effect = DrainEffect(4, EffectTarget.Opponent)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            // Opponent loses life, controller gains life
            result.state.getLife(player2Id) shouldBe initialOpponentLife - 4
            result.state.getLife(player1Id) shouldBe initialControllerLife + 4
        }

        test("deals damage to target creature and gains life") {
            var state = newGame()

            val (creatureId, state1) = state.addCreatureToBattlefield(groundCreatureDef, player2Id)
            state = state1

            val initialLife = state.getLife(player1Id)

            val effect = DrainEffect(3, EffectTarget.AnyTarget)
            val context = ExecutionContext(
                controllerId = player1Id,
                sourceId = player1Id,
                targets = listOf(ResolvedTarget.Permanent(creatureId))
            )

            val result = registry.execute(state, effect, context)

            // Creature should have 3 damage
            result.state.getEntity(creatureId)?.get<DamageComponent>()?.amount shouldBe 3
            // Controller gains 3 life
            result.state.getLife(player1Id) shouldBe initialLife + 3
        }
    }

    context("GainLifeEffect execution") {
        test("Sacred Nectar gains 4 life") {
            var state = newGame()

            val initialLife = state.getLife(player1Id)

            val effect = GainLifeEffect(4, EffectTarget.Controller)
            val context = ExecutionContext(player1Id, player1Id)

            val result = registry.execute(state, effect, context)

            result.state.getLife(player1Id) shouldBe initialLife + 4
        }
    }
})
