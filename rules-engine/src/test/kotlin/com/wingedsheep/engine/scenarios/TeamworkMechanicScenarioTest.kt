package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Teamwork N (CR 702.194, Marvel Super Heroes) end to end.
 *
 * "Teamwork N" is a static ability functioning while the spell is on the stack: "As an additional
 * cost to cast this spell, you may tap any number of creatures you control with total power N or
 * more" (702.194a, following 601.2b and 601.2f–h). Declaring that intention means the spell was
 * cast *using teamwork* (702.194b) — the fact the card's own riders branch on.
 *
 * The mechanic rides the shared optional-additional-cost rail
 * ([KeywordAbility.OptionalAdditionalCost]) under its own [ChoiceSlot.TEAMWORK], so these tests pin
 * both halves: that the selection behaves like the crew payment it reuses (untapped only, projected
 * power, summoning sickness irrelevant), and that "cast using teamwork" stays a *separate* fact
 * from "kicked".
 *
 * 702.194c — a teamwork-only clause with its own target is targeted only on the declared cast — is
 * pinned by the "Teamwork Rally" cases at the end.
 */
class TeamworkMechanicScenarioTest : ScenarioTestBase() {

    // --- Board pieces ---------------------------------------------------------------------------

    /** 1/1 — two of these are needed to reach a teamwork 2 threshold. */
    private val testScout = card("Test Scout") {
        manaCost = "{W}"
        typeLine = "Creature — Human Scout"
        power = 1
        toughness = 1
    }

    /** 3/3 — clears a teamwork 2 threshold alone. */
    private val testBruiser = card("Test Bruiser") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 3
        toughness = 3
    }

    /** 0/4: never contributes power, so it can't pay a teamwork cost by itself. */
    private val testWall = card("Test Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    /** A lord, so a teamwork threshold has to be measured against *projected* power. */
    private val testCaptain = card("Test Captain") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Other creatures you control get +1/+1."
        staticAbility {
            ability = com.wingedsheep.sdk.scripting.ModifyStats(
                powerBonus = 1,
                toughnessBonus = 1,
                filter = com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
                    .OtherCreaturesYouControl,
            )
        }
    }

    /** Pushes a Wall to *negative* projected power, so a candidate can subtract from a raw sum. */
    private val testWallWeakener = card("Test Wall Weakener") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "Walls you control get -3/-0."
        staticAbility {
            ability = com.wingedsheep.sdk.scripting.ModifyStats(
                powerBonus = -3,
                toughnessBonus = 0,
                filter = com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
                    .OtherCreaturesYouControl.withSubtype("Wall"),
            )
        }
    }

    // --- Cards under test -----------------------------------------------------------------------

    /** Helicarrier Strike's shape: a rider read off the spell while it's still on the stack. */
    private val teamworkBolt = card("Teamwork Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any " +
            "number of creatures you control with total power 2 or more.)\n" +
            "Teamwork Bolt deals 2 damage to target creature. If this spell was cast using " +
            "teamwork, it deals 4 damage to that creature instead."
        teamwork(2)
        spell {
            val damaged = target("target creature", TargetCreature())
            effect = Effects.DealDamage(
                com.wingedsheep.sdk.scripting.values.DynamicAmount.Conditional(
                    condition = Conditions.TeamworkWasPaid,
                    ifTrue = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(4),
                    ifFalse = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2),
                ),
                damaged,
            )
        }
    }

    /** The permanent shape: the declaration must survive onto the resolving permanent. */
    private val teamworkBear = card("Teamwork Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any " +
            "number of creatures you control with total power 2 or more.)\n" +
            "When this creature enters, if it was cast using teamwork, put two +1/+1 counters on it."
        teamwork(2)
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            triggerCondition = Conditions.TeamworkWasPaid
            effect = Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
        }
    }

    /**
     * The modal shape MSH prints on five cards (Widow's Bite, HULK SMASH!, Go Nuts!, Murdock's
     * Crusade, Atlantis Attacks): "Choose one. If this spell was cast using teamwork, choose both
     * instead." The mode count is a cast-time `dynamicChooseCount` gated on the declaration itself
     * (CR 702.194b), so the cast handler has to evaluate it against *this cast's* declared slot —
     * the durable cast-choices bag doesn't exist until the spell resolves.
     *
     * Both modes are targetless so the test drives mode selection alone.
     */
    private val teamworkOrders = card("Teamwork Orders") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any " +
            "number of creatures you control with total power 2 or more.)\n" +
            "Choose one. If this spell was cast using teamwork, choose both instead.\n" +
            "• You gain 3 life.\n" +
            "• Each opponent loses 2 life."
        teamwork(2)
        spell {
            modal(
                chooseCount = 2,
                minChooseCount = 1,
                dynamicChooseCount = com.wingedsheep.sdk.scripting.values.DynamicAmount.Conditional(
                    condition = Conditions.TeamworkWasPaid,
                    ifTrue = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(2),
                    ifFalse = com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(1),
                ),
            ) {
                mode("You gain 3 life") {
                    effect = Effects.GainLife(3)
                }
                mode("Each opponent loses 2 life") {
                    effect = Effects.LoseLife(
                        2,
                        EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.EachOpponent),
                    )
                }
            }
        }
    }

    /**
     * CR 702.194c: "If part of a spell's ability has its effect only if teamwork was used to cast
     * it, and that part of the ability includes any targets, the spell's controller chooses those
     * targets only if teamwork was used to cast that spell. Otherwise, the spell is cast as if it
     * did not have those targets."
     *
     * The teamwork-only clause carries a second target, so the plain cast must be *announced* with
     * one target and the declared cast with two — the Goblin Barrage shape, on the teamwork slot.
     */
    private val teamworkRally = card("Teamwork Rally") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any " +
            "number of creatures you control with total power 2 or more.)\n" +
            "Teamwork Rally deals 2 damage to target creature. If this spell was cast using " +
            "teamwork, it also deals 2 damage to target player."
        teamwork(2)
        spell {
            val damaged = target("target creature", TargetCreature())
            effect = Effects.DealDamage(2, damaged)

            val rallyCreature = kickerTarget("creature", TargetCreature())
            val rallyPlayer = kickerTarget("player", com.wingedsheep.sdk.dsl.Targets.Player)
            kickerEffect = Effects.Composite(
                Effects.DealDamage(2, rallyCreature),
                Effects.DealDamage(2, rallyPlayer),
            )
        }
    }

    /** A kicker card on the same rail — the control for "kicked and teamwork are different facts". */
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

    /** "Whenever you cast a kicked spell, …" — must NOT see a teamwork spell (CR 702.194b). */
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

    private fun com.wingedsheep.engine.state.GameState.isTapped(id: EntityId): Boolean =
        getEntity(id)?.has<TappedComponent>() == true

    init {
        cardRegistry.register(testScout)
        cardRegistry.register(testBruiser)
        cardRegistry.register(testWall)
        cardRegistry.register(testCaptain)
        cardRegistry.register(testWallWeakener)
        cardRegistry.register(teamworkBolt)
        cardRegistry.register(teamworkBear)
        cardRegistry.register(teamworkOrders)
        cardRegistry.register(teamworkRally)
        cardRegistry.register(kickerBear)
        cardRegistry.register(kickedWatcher)

        // -----------------------------------------------------------------------------------
        // CR 702.194a — the optional additional cost and what may pay it
        // -----------------------------------------------------------------------------------

        test("several creatures may combine to clear the threshold, and all of them tap") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Scout")
                .withCardOnBattlefield(1, "Test Scout")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scouts = game.findPermanents("Test Scout")
            scouts.size shouldBe 2
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Scout", "Test Scout", targetId = wall)
                .error shouldBe null
            // Both 1/1s are tapped the moment the cost is paid, before the spell resolves.
            scouts.forEach { game.state.isTapped(it) shouldBe true }

            game.resolveStack()
            // 4 damage, not 2 — the 0/4 wall dies.
            game.isOnBattlefield("Test Wall") shouldBe false
        }

        test("one big creature may clear the threshold alone") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Bruiser", targetId = wall)
                .error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Test Wall") shouldBe false
        }

        test("declining the optional cost taps nothing and leaves the rider off") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            game.castSpell(1, "Teamwork Bolt", targetId = wall).error shouldBe null
            game.state.isTapped(bruiser) shouldBe false
            game.resolveStack()

            // Only 2 damage — the 0/4 wall survives.
            game.isOnBattlefield("Test Wall") shouldBe true
            game.state.isTapped(bruiser) shouldBe false
        }

        test("a selection short of the threshold is rejected and nothing is tapped") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Scout")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scout = game.findPermanent("Test Scout").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            // One 1/1 is total power 1 — short of teamwork 2.
            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Scout", targetId = wall)
                .error.shouldNotBeNull()
            game.state.isTapped(scout) shouldBe false
            game.isInHand(1, "Teamwork Bolt") shouldBe true
        }

        test("an already-tapped creature can't pay the cost (CR 701.26a)") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser", tapped = true)
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Bruiser", targetId = wall)
                .error.shouldNotBeNull()
            game.isInHand(1, "Teamwork Bolt") shouldBe true
        }

        test("a creature you don't control can't pay the cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(2, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            val caster = game.player1Id
            val cardId = game.state.getHand(caster).first()

            game.execute(
                com.wingedsheep.engine.core.CastSpell(
                    playerId = caster,
                    cardId = cardId,
                    targets = listOf(
                        com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(wall)
                    ),
                    declaredCostSlot = ChoiceSlot.TEAMWORK,
                    additionalCostPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                        variableCostPermanents = listOf(bruiser)
                    ),
                )
            ).error.shouldNotBeNull()
            game.state.isTapped(bruiser) shouldBe false
        }

        // -----------------------------------------------------------------------------------
        // Projected power, and the summoning-sickness question (CR 302.6 vs a tap cost)
        // -----------------------------------------------------------------------------------

        test("a lord's bonus counts toward the threshold — the sum reads projected power") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Scout")   // 1/1 base, 2/2 under the captain
                .withCardOnBattlefield(1, "Test Captain")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scout = game.findPermanent("Test Scout").shouldNotBeNull()
            game.state.projectedState.getPower(scout) shouldBe 2
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            // The lone Scout is base power 1 but projected power 2 — enough for teamwork 2.
            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Scout", targetId = wall)
                .error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Test Wall") shouldBe false
        }

        test("summoning sickness doesn't stop a creature paying a teamwork cost (crew's rule)") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser", summoningSickness = true, enteredThisTurn = true)
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Bruiser", targetId = wall)
                .error shouldBe null
            game.state.isTapped(bruiser) shouldBe true
            game.resolveStack()
            game.isOnBattlefield("Test Wall") shouldBe false
        }

        // -----------------------------------------------------------------------------------
        // CR 702.194b — the durable "cast using teamwork" fact
        // -----------------------------------------------------------------------------------

        test("the declaration survives onto the resolving permanent") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bear")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithTeamwork(1, "Teamwork Bear", "Test Bruiser").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Teamwork Bear").shouldNotBeNull()
            val slots = game.state.getEntity(bear)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
                ?.chosen?.keys.orEmpty()
            slots shouldContain ChoiceSlot.TEAMWORK
            game.state.getEntity(bear)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("without teamwork the intervening-if trigger never fires") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bear")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Teamwork Bear").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Teamwork Bear").shouldNotBeNull()
            val counters = game.state.getEntity(bear)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            counters shouldBe 0
        }

        test("a teamwork spell is not a kicked spell, and a kicked spell is not teamwork") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bear")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(1, "Kicked Watcher")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .withLifeTotal(1, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithTeamwork(1, "Teamwork Bear", "Test Bruiser").error shouldBe null
            game.resolveStack()

            // "Whenever you cast a kicked spell" must not see the teamwork declaration.
            game.state.lifeTotal(game.player1Id) shouldBe 20
        }

        test("declaring teamwork on a card without it is rejected") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Kicker Bear")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithTeamwork(1, "Kicker Bear", "Test Bruiser").error.shouldNotBeNull()
            game.isInHand(1, "Kicker Bear") shouldBe true
        }

        // -----------------------------------------------------------------------------------
        // Legal actions — the cast variant the client renders
        // -----------------------------------------------------------------------------------

        test("the teamwork cast variant advertises the candidates and the power threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Scout")
                .withCardOnBattlefield(1, "Test Bruiser", tapped = true)
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val teamworkCast = game.getLegalActions(1)
                .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                .shouldNotBeNull()

            val costInfo = teamworkCast.additionalCostInfo.shouldNotBeNull()
            costInfo.tapForPowerRequired shouldBe 2
            // The tapped Bruiser is not a candidate; the untapped Scout is.
            costInfo.tapForPowerCreatures.map { it.name } shouldBe listOf("Test Scout")
            // Total available power (1) is short of 2, so the variant is offered unaffordable.
            teamworkCast.isAffordable shouldBe false
        }

        test("the teamwork cast variant is affordable once the board can reach the threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val teamworkCast = game.getLegalActions(1)
                .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                .shouldNotBeNull()
            teamworkCast.isAffordable shouldBe true
            teamworkCast.description shouldBe "Cast Teamwork Bolt (Teamwork 2)"
        }

        test("a creature at negative power can't drag the affordability ceiling below the threshold") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Bolt")
                .withCardOnBattlefield(1, "Test Bruiser")        // 3/3 — pays teamwork 2 alone
                .withCardOnBattlefield(1, "Test Wall")           // 0/4, dragged to -3/4 below
                .withCardOnBattlefield(1, "Test Wall Weakener")  // 1/1, "Walls you control get -3/-0"
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Raw sum over every candidate is 3 + (-3) + 1 = 1, below the threshold; the reachable
            // ceiling is 3 + 0 + 1 = 4, because nothing forces the debuffed Wall into the payment.
            val teamworkCast = game.getLegalActions(1)
                .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                .shouldNotBeNull()
            teamworkCast.isAffordable shouldBe true

            val wall = game.findPermanents("Test Wall").last()
            game.castSpellWithTeamwork(1, "Teamwork Bolt", "Test Bruiser", targetId = wall)
                .error shouldBe null
        }

        // -----------------------------------------------------------------------------------
        // CR 702.194b + 700.2 — "Choose one. If this spell was cast using teamwork, choose both
        // instead": the mode count branches on *this cast's* declaration, read at cast time.
        // -----------------------------------------------------------------------------------

        test("a modal teamwork spell chooses both modes when teamwork is declared") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Orders")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            game.castSpellWithTeamwork(
                1, "Teamwork Orders", "Test Bruiser",
                chosenModes = listOf(0, 1),
            ).error shouldBe null
            game.state.isTapped(bruiser) shouldBe true

            game.resolveStack()
            game.state.lifeTotal(game.player1Id) shouldBe 23
            game.state.lifeTotal(game.player2Id) shouldBe 18
        }

        test("the same spell cast without teamwork is held to one mode") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Orders")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withLifeTotal(1, 20)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val caster = game.player1Id
            val cardId = game.state.getHand(caster).first()

            // Both modes without the declaration: the dynamic count evaluates to 1, so this is
            // rejected outright rather than silently resolving both halves.
            game.execute(
                com.wingedsheep.engine.core.CastSpell(
                    playerId = caster,
                    cardId = cardId,
                    chosenModes = listOf(0, 1),
                )
            ).error.shouldNotBeNull()
            game.isInHand(1, "Teamwork Orders") shouldBe true

            // One mode is fine.
            game.execute(
                com.wingedsheep.engine.core.CastSpell(
                    playerId = caster,
                    cardId = cardId,
                    chosenModes = listOf(0),
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.lifeTotal(game.player1Id) shouldBe 23
            game.state.lifeTotal(game.player2Id) shouldBe 20
        }

        test("the modal teamwork cast variant carries the modes and the teamwork cost") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Orders")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Without this the declared variant would be advertised as a plain no-mode cast and
            // every submit would fail with "Too few modes chosen".
            val teamworkCast = game.getLegalActions(1)
                .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                .shouldNotBeNull()
            teamworkCast.description shouldBe "Cast Teamwork Orders (Teamwork 2)"
            val modal = teamworkCast.modalEnumeration.shouldNotBeNull()
            modal.chooseCount shouldBe 2
            modal.minChooseCount shouldBe 1
            modal.modes.map { it.description } shouldBe
                listOf("You gain 3 life", "Each opponent loses 2 life")
            modal.modes.all { it.available } shouldBe true
        }

        // -----------------------------------------------------------------------------------
        // CR 702.194c — a teamwork-only clause that has its own target. Its target is chosen
        // only on the declared cast; the plain cast is announced as if the clause weren't there.
        // -----------------------------------------------------------------------------------

        test("only the teamwork cast is announced with the clause's extra target") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Rally")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val casts = game.getLegalActions(1).filter { it.description.startsWith("Cast Teamwork Rally") }

            // The plain cast has one requirement, so it rides the single-requirement fields.
            val plainCast = casts.firstOrNull { it.additionalCostInfo == null }.shouldNotBeNull()
            plainCast.targetRequirements shouldBe null
            plainCast.targetCount shouldBe 1

            // The declared cast announces both — "target creature" and "target player".
            val teamworkCast = casts
                .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                .shouldNotBeNull()
            teamworkCast.targetRequirements.shouldNotBeNull().size shouldBe 2
        }

        test("the plain cast resolves with only the base target and spares the player") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Rally")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            game.castSpell(1, "Teamwork Rally", targetId = wall).error shouldBe null
            game.resolveStack()

            // The 0/4 Wall survives 2 damage; the point is that no second target was ever asked
            // for, so the opponent is untouched.
            game.isOnBattlefield("Test Wall") shouldBe true
            game.state.lifeTotal(game.player2Id) shouldBe 20
            game.state.isTapped(bruiser) shouldBe false
        }

        test("the teamwork cast chooses the extra target and damages it too") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Rally")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withLifeTotal(2, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()
            val caster = game.player1Id
            val cardId = game.state.getHand(caster).first()

            game.execute(
                com.wingedsheep.engine.core.CastSpell(
                    playerId = caster,
                    cardId = cardId,
                    targets = listOf(
                        com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(wall),
                        com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id),
                    ),
                    declaredCostSlot = ChoiceSlot.TEAMWORK,
                    additionalCostPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                        variableCostPermanents = listOf(bruiser)
                    ),
                )
            ).error shouldBe null
            game.state.isTapped(bruiser) shouldBe true

            game.resolveStack()
            game.state.lifeTotal(game.player2Id) shouldBe 18
        }

        test("the teamwork cast is rejected when the clause's target is missing") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Teamwork Rally")
                .withCardOnBattlefield(1, "Test Bruiser")
                .withCardOnBattlefield(2, "Test Wall")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bruiser = game.findPermanent("Test Bruiser").shouldNotBeNull()
            val wall = game.findPermanent("Test Wall").shouldNotBeNull()

            // Declaring teamwork but announcing only the base target — the clause's target is
            // mandatory once teamwork was used (CR 702.194c), so the cast is rewound.
            game.castSpellWithTeamwork(1, "Teamwork Rally", "Test Bruiser", targetId = wall)
                .error.shouldNotBeNull()
            game.state.isTapped(bruiser) shouldBe false
            game.isInHand(1, "Teamwork Rally") shouldBe true
        }
    }
}
