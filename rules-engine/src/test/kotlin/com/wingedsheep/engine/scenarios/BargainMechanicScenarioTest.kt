package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.wasKickedChoice
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bargain (CR 702.166, Wilds of Eldraine) end to end.
 *
 * "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)" — an
 * optional additional cost declared as the spell is cast (702.166a, following 601.2b) whose
 * declaration makes the spell *bargained* (702.166b). A card's "if it was bargained" abilities are
 * linked to its own bargain ability (702.166c), and a bargain-only clause's targets are chosen only
 * when the spell is bargained (702.166d).
 *
 * The mechanic rides the shared optional-additional-cost rail
 * ([KeywordAbility.OptionalAdditionalCost]) under its own [ChoiceSlot.BARGAINED], so these tests pin
 * both halves: that bargaining behaves like the printed cards, and that "bargained" stays a
 * *separate* fact from "kicked" in both directions.
 */
class BargainMechanicScenarioTest : ScenarioTestBase() {

    // --- Sacrifice fodder, kept inert so a test only ever observes the mechanic ----------------

    private val testTrinket = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private val testCharm = card("Test Charm") {
        manaCost = "{1}"
        typeLine = "Enchantment"
    }

    /** A 0/4 wall: survives 2 damage, dies to 4 — the discriminator for the bargained rider. */
    private val testWall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    // --- Cards under test ----------------------------------------------------------------------

    // Torch the Tower's shape: a rider on the spell's own effect, read while the spell is still on
    // the stack — there is no permanent yet, so no durable choice bag to read from.
    private val bargainBolt = card("Bargain Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast " +
            "this spell.)\nBargain Bolt deals 2 damage to target creature. If this spell was " +
            "bargained, it deals 4 damage to that creature instead."
        bargain()
        spell {
            val damaged = target("target creature", TargetCreature())
            effect = Effects.Composite(
                Effects.DealDamage(2, damaged),
                ConditionalEffect(
                    condition = Conditions.WasBargained,
                    effect = Effects.DealDamage(2, damaged),
                ),
            )
        }
    }

    // Agatha's Champion's shape: the fact rides the resolving permanent, and the enters trigger has
    // an intervening-if on it (CR 603.4 — unbargained, it never goes on the stack at all).
    private val bargainBear = card("Bargain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast " +
            "this spell.)\nWhen this creature enters, if it was bargained, put two +1/+1 counters " +
            "on it."
        bargain()
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = Conditions.WasBargained
            effect = Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
        }
    }

    // Hamlet Glutton's shape: the cast that declares bargain is priced {2} cheaper. The gate is
    // evaluated against the branch being priced, before the spell exists as an object.
    private val bargainGiant = card("Bargain Giant") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Giant"
        power = 5
        toughness = 5
        oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast " +
            "this spell.)\nThis spell costs {2} less to cast if it's bargained."
        bargain()
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                modification = CostModification.ReduceGeneric(2),
                gating = CostGating.OnlyIf(Conditions.WasBargained),
            )
        }
    }

    // Brave the Wilds' shape (CR 702.166d): the bargained branch has a target the plain cast
    // doesn't, so the plain cast is announced as if that clause weren't there.
    private val bargainSurvey = card("Bargain Survey") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast " +
            "this spell.)\nIf this spell was bargained, target creature gets +3/+3 until end of " +
            "turn.\nYou gain 1 life."
        bargain()
        spell {
            effect = Effects.GainLife(1)
            val pumped = kickerTarget("target creature", TargetCreature())
            kickerEffect = Effects.Composite(
                Effects.ModifyStats(3, 3, pumped),
                Effects.GainLife(1),
            )
        }
    }

    // A kicker card on the same rail — the control for "kicked and bargained are different facts".
    private val kickerBear = card("Kicker Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Kicker {1}\nWhen this creature enters, if it was kicked, put two +1/+1 " +
            "counters on it."
        keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{1}")))
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = WasKicked
            effect = Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
        }
    }

    // Cackling Witch's payoff shape: "Whenever you cast a kicked spell, …". A bargained spell must
    // not satisfy it (CR 702.166c — bargain's payoffs are linked to bargain).
    private val kickedWatcher = card("Kicked Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "Whenever you cast a kicked spell, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.youCastSpell(requires = setOf(SpellCastPredicate.WasKicked))
            effect = Effects.GainLife(3)
        }
    }

    private fun ComponentContainer.declaredBargain(): Boolean =
        get<CastChoicesComponent>()?.chosen?.containsKey(ChoiceSlot.BARGAINED) == true

    private fun ComponentContainer.plusOneCounters(): Int =
        get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        cardRegistry.register(testTrinket)
        cardRegistry.register(testCharm)
        cardRegistry.register(testWall)
        cardRegistry.register(bargainBolt)
        cardRegistry.register(bargainBear)
        cardRegistry.register(bargainGiant)
        cardRegistry.register(bargainSurvey)
        cardRegistry.register(kickerBear)
        cardRegistry.register(kickedWatcher)

        // ---------------------------------------------------------------------------------------
        // CR 702.166a — the optional additional cost, and what may pay it
        // ---------------------------------------------------------------------------------------

        test("bargaining sacrifices the chosen artifact and the bargained rider applies") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bolt")
                .withCardOnBattlefield(1, "Test Trinket")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpellBargained(1, "Bargain Bolt", "Test Trinket", targetId = wall)
                .error shouldBe null
            game.resolveStack()

            // The artifact paid the cost, and 2 + 2 damage killed the 0/4.
            game.isInGraveyard(1, "Test Trinket") shouldBe true
            game.isOnBattlefield("Test Wall") shouldBe false
        }

        test("declining bargain leaves the rider off — only the printed 2 damage") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bolt")
                .withCardOnBattlefield(1, "Test Trinket")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpell(1, "Bargain Bolt", targetId = wall).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Trinket") shouldBe false
            game.isOnBattlefield("Test Wall") shouldBe true
        }

        test("an enchantment may pay the bargain cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Charm")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellBargained(1, "Bargain Bear", "Test Charm").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Charm") shouldBe true
            game.findPermanent("Bargain Bear").shouldNotBeNull()
        }

        test("a token may pay the bargain cost even though 'token' is not a card type") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Wall", isToken = true)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellBargained(1, "Bargain Bear", "Test Wall").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Test Wall") shouldBe false
            game.findPermanent("Bargain Bear").shouldNotBeNull()
        }

        test("a nontoken creature cannot pay the bargain cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Wall")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellBargained(1, "Bargain Bear", "Test Wall").error.shouldNotBeNull()
            game.isOnBattlefield("Test Wall") shouldBe true
            game.isInHand(1, "Bargain Bear") shouldBe true
        }

        test("the bargained cast is unaffordable with nothing to sacrifice, offered with fodder") {
            val withoutFodder = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // CR 601.2b lets a player announce the cost, so the variant is still enumerated — but
            // with no legal sacrifice it is never payable, and the client can't submit it.
            val unpayable = withoutFodder.getLegalActions(1)
                .filter { it.actionType == "CastWithKicker" }
            unpayable shouldHaveSize 1
            unpayable.single().isAffordable shouldBe false

            val withFodder = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val offered = withFodder.getLegalActions(1).filter { it.actionType == "CastWithKicker" }
            offered.map { it.description } shouldContain "Cast Bargain Bear (Bargained)"
            offered.single().isAffordable shouldBe true
        }

        test("the sacrifice is a cost — it is paid before the spell is even on the stack") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellBargained(1, "Bargain Bear", "Test Trinket").error shouldBe null

            // Still on the stack, yet the artifact is already gone (CR 601.2h) — so countering the
            // spell or letting it fizzle can never refund the bargain.
            game.isInGraveyard(1, "Test Trinket") shouldBe true
            game.state.stack.any { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Bargain Bear"
            } shouldBe true
        }

        // ---------------------------------------------------------------------------------------
        // CR 702.166b/c — "bargained" is its own durable fact, linked to this card's own abilities
        // ---------------------------------------------------------------------------------------

        test("a bargained permanent carries the fact, so its intervening-if enters trigger fires") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellBargained(1, "Bargain Bear", "Test Trinket").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Bargain Bear").shouldNotBeNull()
            val entity = game.state.getEntity(bear).shouldNotBeNull()
            entity.declaredBargain() shouldBe true
            // Bargained is not kicked (CR 702.166c) — the KICKED slot stays empty.
            entity.wasKickedChoice() shouldBe false
            entity.plusOneCounters() shouldBe 2
        }

        test("an unbargained permanent's intervening-if trigger never goes on the stack") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bargain Bear").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Bargain Bear").shouldNotBeNull()
            val entity = game.state.getEntity(bear).shouldNotBeNull()
            entity.declaredBargain() shouldBe false
            entity.plusOneCounters() shouldBe 0
            game.isInGraveyard(1, "Test Trinket") shouldBe false
        }

        test("a kicked spell does not read as bargained") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Kicker Bear")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Kicker Bear").single()
            game.execute(CastSpell(game.player1Id, cardId, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Kicker Bear").shouldNotBeNull()
            val entity = game.state.getEntity(bear).shouldNotBeNull()
            entity.wasKickedChoice() shouldBe true
            entity.declaredBargain() shouldBe false
            // The kicker payoff still fires for the mechanic that owns it.
            entity.plusOneCounters() shouldBe 2
        }

        test("'whenever you cast a kicked spell' ignores a bargained spell but sees a kicked one") {
            val bargained = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Bear")
                .withCardOnBattlefield(1, "Kicked Watcher")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            bargained.castSpellBargained(1, "Bargain Bear", "Test Trinket").error shouldBe null
            bargained.resolveStack()
            bargained.getLifeTotal(1) shouldBe 20

            val kicked = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Kicker Bear")
                .withCardOnBattlefield(1, "Kicked Watcher")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kickerBearId = kicked.findCardsInHand(1, "Kicker Bear").single()
            kicked.execute(
                CastSpell(kicked.player1Id, kickerBearId, declaredCostSlot = ChoiceSlot.KICKED)
            ).error shouldBe null
            kicked.resolveStack()
            kicked.getLifeTotal(1) shouldBe 23
        }

        test("a card without bargain rejects a cast claiming to be bargained") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Test Wall")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Test Wall").single()
            game.execute(CastSpell(game.player1Id, cardId, declaredCostSlot = ChoiceSlot.BARGAINED))
                .error.shouldNotBeNull()
        }

        // ---------------------------------------------------------------------------------------
        // "This spell costs {2} less to cast if it's bargained"
        // ---------------------------------------------------------------------------------------

        test("the bargained cast is priced {2} cheaper; the plain cast keeps the printed cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Giant")
                .withCardOnBattlefield(1, "Test Trinket")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val actions = game.getLegalActions(1)
            val plain = actions.single {
                it.actionType == "CastSpell" && it.description == "Cast Bargain Giant"
            }
            val bargainedCast = actions.single { it.actionType == "CastWithKicker" }

            plain.manaCostString shouldBe "{5}{G}"
            plain.isAffordable shouldBe false // four lands can't pay {5}{G}
            bargainedCast.manaCostString shouldBe "{3}{G}"
            bargainedCast.isAffordable shouldBe true

            game.castSpellBargained(1, "Bargain Giant", "Test Trinket").error shouldBe null
            game.resolveStack()
            game.findPermanent("Bargain Giant").shouldNotBeNull()
        }

        // ---------------------------------------------------------------------------------------
        // CR 702.166d — a target that exists only on the bargained branch
        // ---------------------------------------------------------------------------------------

        test("a bargain-only target is chosen only when the spell is bargained") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Survey")
                .withCardOnBattlefield(1, "Test Trinket")
                .withCardOnBattlefield(1, "Test Wall")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val actions = game.getLegalActions(1)
            // The plain cast is announced as though the bargain clause — and its target — weren't
            // there; only the bargained variant asks for the creature to pump.
            actions.single { it.actionType == "CastSpell" && it.description == "Cast Bargain Survey" }
                .requiresTargets shouldBe false
            actions.single { it.actionType == "CastWithKicker" }.requiresTargets shouldBe true

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpellBargained(1, "Bargain Survey", "Test Trinket", targetId = wall)
                .error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(wall) shouldBe 3
            game.getLifeTotal(1) shouldBe 21
        }

        test("the plain cast of a bargain-only-target spell resolves with no target") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bargain Survey")
                .withCardOnBattlefield(1, "Test Wall")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpell(1, "Bargain Survey").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 21
            game.state.projectedState.getPower(wall) shouldBe 0
        }
    }
}
