package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.UntargetableBy
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [CantBeTargetedBy] — "can't be the target of Aura spells" (Tetsuo Umezawa) and "can't be the
 * target of spells that can target only Walls or of abilities that can target only Walls" (Wall of
 * Shadows). Pins every targeting site: legal-target enumeration, cast/activation validation, and
 * the resolution re-check — and that neither form is controller-gated.
 */
class CantBeTargetedByKindTest : FunSpec({

    val auraProof = card("Test Aura-Proof Archer") {
        manaCost = "{2}"
        typeLine = "Creature — Human Archer"
        power = 3
        toughness = 3
        staticAbility { ability = CantBeTargetedBy(UntargetableBy.SpellsWithSubtype(Subtype.AURA)) }
    }
    val shadowWall = card("Test Shadow Wall") {
        manaCost = "{2}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 1
        staticAbility { ability = CantBeTargetedBy(UntargetableBy.TargetingOnly(Subtype.WALL)) }
    }
    val plainWall = card("Test Plain Wall") {
        manaCost = "{2}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }
    val strength = card("Test Strength") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = ModifyStats(2, 2) }
    }
    val wallBreaker = card("Test Wall Breaker") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
            effect = Effects.Destroy(t)
        }
    }
    val murder = card("Test Murder") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }
    val wallSmasher = card("Test Wall Smasher") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
            effect = Effects.Destroy(t)
        }
    }
    val pinger = card("Test Kind Pinger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    // "Creatures you control can't be the target of Aura spells" — lets a test add the
    // restriction after the Aura has already targeted.
    val auraWardLord = card("Test Aura Ward Lord") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CantBeTargetedBy(
                UntargetableBy.SpellsWithSubtype(Subtype.AURA),
                GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    val wallWardLord = card("Test Wall Ward Lord") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        staticAbility {
            ability = CantBeTargetedBy(
                UntargetableBy.TargetingOnly(Subtype.WALL),
                GroupFilter(GameObjectFilter.Creature.withSubtype("Wall").youControl())
            )
        }
    }

    // A disturb card whose back face is an Aura (Lanterns' Lift's shape): in the graveyard the
    // entity is its front face, a Spirit creature, but the spell cast is the Aura back.
    val lanternFront = card("Test Ward Lantern") {
        manaCost = "{U}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        disturb("{1}")
    }
    val lanternBack = card("Test Ward Lantern Aura") {
        manaCost = ""
        colorIndicator = "U"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        staticAbility { ability = ModifyStats(1, 1) }
    }
    val lantern = CardDefinition.doubleFacedPermanent(lanternFront, lanternBack)

    fun driver() = GameTestDriver().also {
        it.registerCards(
            TestCards.all + listOf(auraProof, shadowWall, plainWall, strength, wallBreaker, murder, wallSmasher, pinger, auraWardLord, wallWardLord, lantern)
        )
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val smashId = wallSmasher.script.activatedAbilities.single().id
    val pingId = pinger.script.activatedAbilities.single().id

    fun GameTestDriver.offeredTargets(player: EntityId, matches: (GameAction) -> Boolean) =
        legalActions(player).first { matches(it.action) }.let { la ->
            la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty()
        }
    fun GameTestDriver.offeredToCast(player: EntityId, cardId: EntityId) =
        offeredTargets(player) { it is CastSpell && it.cardId == cardId }

    test("description renders the oracle phrasing") {
        CantBeTargetedBy(UntargetableBy.SpellsWithSubtype(Subtype.AURA)).description shouldBe
            "This creature can't be the target of Aura spells"
        CantBeTargetedBy(UntargetableBy.TargetingOnly(Subtype.WALL)).description shouldBe
            "This creature can't be the target of spells that can target only Walls or of abilities that can target only Walls"
    }

    context("Aura spells") {
        test("an opponent's Aura spell can't target it and it isn't offered; a non-Aura spell can") {
            val d = driver()
            val archer = d.putCreatureOnBattlefield(d.player2, auraProof.name)
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val aura = d.putCardInHand(d.player1, strength.name)
            d.giveColorlessMana(d.player1, 1)
            val offered = d.offeredToCast(d.player1, aura)
            offered.contains(archer) shouldBe false
            offered.contains(bear) shouldBe true
            d.castSpell(d.player1, aura, listOf(archer)).error shouldNotBe null

            val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
            d.giveMana(d.player1, Color.RED, 1)
            d.offeredToCast(d.player1, bolt).contains(archer) shouldBe true
            d.castSpell(d.player1, bolt, listOf(archer)).error shouldBe null
        }

        test("a disturb Aura back cast from the graveyard is an Aura spell, so it can't target it") {
            val d = driver()
            val archer = d.putCreatureOnBattlefield(d.player2, auraProof.name)
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val card = d.putCardInGraveyard(d.player1, lanternFront.name)
            d.giveColorlessMana(d.player1, 1)
            val offered = d.offeredTargets(d.player1) {
                it is CastSpell && it.cardId == card && it.alternativeCostType == AlternativeCostType.DISTURB
            }
            offered.contains(archer) shouldBe false
            offered.contains(bear) shouldBe true
            d.submit(
                CastSpell(
                    d.player1, card, listOf(ChosenTarget.Permanent(archer)),
                    useAlternativeCost = true, alternativeCostType = AlternativeCostType.DISTURB,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldNotBe null
        }

        test("not controller-gated: its own controller's Aura spell can't target it either") {
            val d = driver()
            val archer = d.putCreatureOnBattlefield(d.player1, auraProof.name)
            // A legal host keeps the cast on offer, so the archer's absence is what's asserted.
            d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val aura = d.putCardInHand(d.player1, strength.name)
            d.giveColorlessMana(d.player1, 1)
            d.offeredToCast(d.player1, aura).contains(archer) shouldBe false
            d.castSpell(d.player1, aura, listOf(archer)).error shouldNotBe null
        }

        test("an ability can target it") {
            val d = driver()
            val archer = d.putCreatureOnBattlefield(d.player2, auraProof.name)
            val source = d.putPermanentOnBattlefield(d.player1, pinger.name)
            d.giveColorlessMana(d.player1, 1)
            d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == source }.contains(archer) shouldBe true
            d.submit(ActivateAbility(d.player1, source, pingId, listOf(ChosenTarget.Permanent(archer)))).error shouldBe null
        }

        test("gaining the restriction after being targeted makes the Aura spell fizzle on resolution") {
            val d = driver()
            val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val aura = d.putCardInHand(d.player1, strength.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, aura, listOf(bear)).error shouldBe null
            d.putPermanentOnBattlefield(d.player2, auraWardLord.name)
            d.bothPass()
            d.findPermanent(d.player1, strength.name) shouldBe null
            d.getGraveyardCardNames(d.player1).contains(strength.name) shouldBe true
            d.state.getBattlefield().none { d.state.getEntity(it)?.get<AttachedToComponent>()?.targetId == bear } shouldBe true
        }
    }

    context("spells and abilities that can target only Walls") {
        test("a spell that can target only Walls can't target it, but can target another Wall") {
            val d = driver()
            val shadow = d.putCreatureOnBattlefield(d.player2, shadowWall.name)
            val other = d.putCreatureOnBattlefield(d.player2, plainWall.name)
            val spell = d.putCardInHand(d.player1, wallBreaker.name)
            d.giveColorlessMana(d.player1, 1)
            val offered = d.offeredToCast(d.player1, spell)
            offered.contains(shadow) shouldBe false
            offered.contains(other) shouldBe true
            d.castSpell(d.player1, spell, listOf(shadow)).error shouldNotBe null
        }

        test("its own controller's Wall-only spell can't target it either") {
            val d = driver()
            val shadow = d.putCreatureOnBattlefield(d.player1, shadowWall.name)
            val spell = d.putCardInHand(d.player1, wallBreaker.name)
            d.putCreatureOnBattlefield(d.player1, plainWall.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, spell, listOf(shadow)).error shouldNotBe null
        }

        test("a 'target creature' spell can target it") {
            val d = driver()
            val shadow = d.putCreatureOnBattlefield(d.player2, shadowWall.name)
            val spell = d.putCardInHand(d.player1, murder.name)
            d.giveColorlessMana(d.player1, 1)
            d.offeredToCast(d.player1, spell).contains(shadow) shouldBe true
            d.castSpell(d.player1, spell, listOf(shadow)).error shouldBe null
            d.bothPass()
            d.findPermanent(d.player2, shadowWall.name) shouldBe null
        }

        test("an ability that can target only Walls can't target it; an 'any target' ability can") {
            val d = driver()
            val shadow = d.putCreatureOnBattlefield(d.player2, shadowWall.name)
            val other = d.putCreatureOnBattlefield(d.player2, plainWall.name)
            val smasher = d.putPermanentOnBattlefield(d.player1, wallSmasher.name)
            val pingSource = d.putPermanentOnBattlefield(d.player1, pinger.name)
            d.giveColorlessMana(d.player1, 2)
            val offered = d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == smasher }
            offered.contains(shadow) shouldBe false
            offered.contains(other) shouldBe true
            d.submit(ActivateAbility(d.player1, smasher, smashId, listOf(ChosenTarget.Permanent(shadow)))).error shouldNotBe null
            d.offeredTargets(d.player1) { it is ActivateAbility && it.sourceId == pingSource }.contains(shadow) shouldBe true
        }

        test("a Wall that gains the restriction after being targeted makes the Wall-only spell fizzle") {
            val d = driver()
            val wall = d.putCreatureOnBattlefield(d.player2, plainWall.name)
            val spell = d.putCardInHand(d.player1, wallBreaker.name)
            d.giveColorlessMana(d.player1, 1)
            d.castSpell(d.player1, spell, listOf(wall)).error shouldBe null
            d.putPermanentOnBattlefield(d.player2, wallWardLord.name)
            d.bothPass()
            d.state.getBattlefield().contains(wall) shouldBe true
        }
    }
})
