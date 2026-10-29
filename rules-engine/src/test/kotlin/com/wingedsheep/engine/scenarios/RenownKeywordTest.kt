package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mechanic-level tests for Renown N (CR 702.112).
 *
 * A renown card declares one keyword ability; the engine supplies the printed triggered ability
 * from [com.wingedsheep.sdk.scripting.Renown] — "When this creature deals combat damage to a
 * player, if it isn't renowned, put N +1/+1 counters on it and it becomes renowned"
 * (CR 702.112a) — the same keyword-expansion shape as vanishing, fabricate and flanking.
 *
 * The spec being pinned here, clause by clause:
 *  - **702.112a** — the trigger is *combat* damage dealt to a *player*, and it puts exactly N
 *    counters on.
 *  - **702.112a's intervening-`if`** — "if it isn't renowned" gates the trigger, so a creature that
 *    connects twice is renowned once and gets N counters, not 2N.
 *  - **702.112b** — renowned is a designation that lasts *until it leaves the battlefield*, is not
 *    an ability, and is not a copiable value.
 *  - **702.112c** — multiple instances trigger separately, but only the first to resolve does
 *    anything.
 *  - the projected-keyword gate — a creature that has lost all abilities has no renown trigger.
 *  - **granted renown** (Aragorn, Hornburg Hero's "attacking creatures you control have … renown
 *    1") — the projected `RENOWN_<n>` derives its own trigger per grant instance, beside printed
 *    renown, and never fuses with it or with an identical grant.
 */
class RenownKeywordTest : FunSpec({

    /** Renown 1, and enough toughness to survive its own attacks. */
    val renownedScout = card("Test Renowned Scout") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Scout"
        power = 2
        toughness = 4
        oracleText = "Renown 1"
        keywordAbility(KeywordAbility.renown(1))
    }

    /** Renown 2 — proves N is read off the printed keyword, not hardcoded. */
    val renownedCaptain = card("Test Renowned Captain") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 5
        oracleText = "Renown 2"
        keywordAbility(KeywordAbility.renown(2))
    }

    /** Two printed instances — CR 702.112c's "each triggers separately". */
    val twiceRenowned = card("Test Twice-Renowned Hero") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 5
        oracleText = "Renown 1\nRenown 2"
        keywordAbility(KeywordAbility.renown(1))
        keywordAbility(KeywordAbility.renown(2))
    }

    /**
     * A renown payoff that reads the designation back — the Goblin Glory Chaser shape,
     * "as long as this creature is renowned, it has menace".
     */
    val gloryChaser = card("Test Glory Chaser") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 1
        toughness = 3
        oracleText = "Renown 1\nAs long as this creature is renowned, it has menace."
        keywordAbility(KeywordAbility.renown(1))
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantKeyword(Keyword.MENACE.name, GroupFilter.source()),
                condition = Conditions.SourceIsRenowned,
            )
        }
    }

    /** Bounces a creature — the zone change CR 702.112b says clears the designation. */
    val recall = card("Test Recall") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Return target creature to its owner's hand."
        spell {
            val victim = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(victim)
        }
    }

    /** The Aragorn, Hornburg Hero grant on its own: "Attacking creatures you control have renown 1." */
    val herald = card("Test Renown Herald") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Advisor"
        power = 0
        toughness = 4
        oracleText = "Attacking creatures you control have renown 1."
        staticAbility {
            ability = GrantKeyword(
                KeywordAbility.grantedRenown(1),
                GroupFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.attacking().youControl()),
            )
        }
    }

    fun createDriver(vararg extra: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(renownedScout, renownedCaptain, twiceRenowned, gloryChaser, recall, herald) + extra
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, perm: EntityId): Int =
        driver.state.getEntity(perm)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun isRenowned(driver: GameTestDriver, perm: EntityId): Boolean =
        driver.state.getEntity(perm)?.has<RenownedComponent>() == true

    /**
     * Pass priority through combat, auto-answering the ordering and damage-assignment decisions,
     * until the postcombat main phase. Mirrors `FlankingCombatTest.resolveThroughCombat`; renown's
     * trigger resolves inside this window. Returns the largest stack seen — in these tests every
     * stack object is a renown trigger, so that is how many renown abilities triggered at once.
     */
    fun resolveThroughCombat(driver: GameTestDriver): Int {
        var guard = 0
        var maxStack = 0
        while (driver.currentStep != Step.POSTCOMBAT_MAIN && guard++ < 300) {
            maxStack = maxOf(maxStack, driver.state.stack.size)
            when (val decision = driver.state.pendingDecision) {
                is OrderObjectsDecision ->
                    driver.submitDecision(decision.playerId, OrderedResponse(decision.id, decision.objects))
                is CombatResolutionDecision -> {
                    val edges = decision.edges.map { DamageEdgeAmount(it.id, it.amount) }
                    driver.submitDecision(decision.playerId, CombatResolutionResponse(decision.id, edges))
                }
                null -> {
                    val priority = driver.state.priorityPlayerId ?: break
                    driver.submit(PassPriority(priority))
                }
                else -> error("Unexpected decision during combat: ${decision::class.simpleName}")
            }
            if (driver.state.gameOver) break
        }
        return maxStack
    }

    /**
     * Attack the opponent unblocked with [attackers] and resolve the whole combat; [duringCombat]
     * runs once blockers are declared. Returns [resolveThroughCombat]'s largest stack.
     */
    fun attackUnblocked(
        driver: GameTestDriver,
        attacker: EntityId,
        attackers: List<EntityId>,
        duringCombat: () -> Unit = {},
    ): Int {
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, attackers, defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, emptyMap()).outcome shouldBe Outcome.Done
        duringCombat()
        return resolveThroughCombat(driver)
    }

    /** Give a driver-minted permanent an explicit timestamp, so layer-6 order is pinned (CR 613.7). */
    fun stamp(driver: GameTestDriver, perm: EntityId, timestamp: Long) {
        driver.replaceState(driver.state.updateEntity(perm) { it.with(TimestampComponent(timestamp)) })
    }

    /** "This creature loses all abilities" as a layer-6 effect with the given [timestamp]. */
    fun loseAllAbilities(driver: GameTestDriver, perm: EntityId, controller: EntityId, timestamp: Long) {
        driver.replaceState(
            driver.state.addFloatingEffects(
                listOf(
                    ActiveFloatingEffect(
                        id = EntityId.generate(),
                        effect = FloatingEffectData(
                            layer = Layer.ABILITY,
                            modification = SerializableModification.RemoveAllAbilities,
                            affectedEntities = setOf(perm),
                        ),
                        duration = Duration.Permanent,
                        sourceId = perm,
                        sourceName = "loses all abilities",
                        controllerId = controller,
                        timestamp = timestamp,
                    )
                )
            )
        )
    }

    test("CR 702.112a — combat damage to a player puts N counters on and renowns the creature") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        driver.removeSummoningSickness(scout)

        attackUnblocked(driver, player, listOf(scout))

        plusOneCounters(driver, scout) shouldBe 1
        isRenowned(driver, scout) shouldBe true
    }

    test("CR 702.112a — N comes from the printed keyword, so renown 2 puts two counters on") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val captain = driver.putCreatureOnBattlefield(player, "Test Renowned Captain")
        driver.removeSummoningSickness(captain)

        attackUnblocked(driver, player, listOf(captain))

        plusOneCounters(driver, captain) shouldBe 2
        isRenowned(driver, captain) shouldBe true
    }

    test("CR 702.112a — combat damage to a blocking creature does not renown the attacker") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val defender = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        val wall = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(scout)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(scout), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(wall to listOf(scout))).outcome shouldBe Outcome.Done
        resolveThroughCombat(driver)

        // All of the Scout's damage went to the blocker, none to the player.
        plusOneCounters(driver, scout) shouldBe 0
        isRenowned(driver, scout) shouldBe false
    }

    test("CR 702.112a's intervening-if — an already-renowned creature gains nothing") {
        // The rule the second connection depends on, tested directly: stamp the designation the
        // way a first connection would have, then swing. A multi-turn re-attack would prove the
        // same clause, but the trigger's gate is what is actually under test.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        driver.removeSummoningSickness(scout)
        driver.replaceState(driver.state.updateEntity(scout) { it.with(RenownedComponent) })

        attackUnblocked(driver, player, listOf(scout))

        // "if it isn't renowned" was false, so the ability never triggered: no counters at all.
        plusOneCounters(driver, scout) shouldBe 0
        isRenowned(driver, scout) shouldBe true
    }

    test("CR 702.112c — two instances trigger separately, but only the first to resolve does anything") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val hero = driver.putCreatureOnBattlefield(player, "Test Twice-Renowned Hero")
        driver.removeSummoningSickness(hero)

        attackUnblocked(driver, player, listOf(hero))

        // Renown 1 and renown 2 both trigger; whichever resolves first renowns the Hero and the
        // other finds its intervening-if false. So the Hero ends on 1 or 2 counters — never 3.
        plusOneCounters(driver, hero) shouldNotBe 3
        (plusOneCounters(driver, hero) in 1..2) shouldBe true
        isRenowned(driver, hero) shouldBe true
    }

    test("CR 702.112b — the designation is lost when the creature leaves the battlefield") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        driver.removeSummoningSickness(scout)

        attackUnblocked(driver, player, listOf(scout))
        isRenowned(driver, scout) shouldBe true

        // Bounce it and replay it: CR 400.7 makes it a new object with no designation.
        driver.giveMana(player, Color.BLUE, 3)
        val recallCard = driver.putCardInHand(player, "Test Recall")
        driver.submit(
            CastSpell(player, recallCard, listOf(ChosenTarget.Permanent(scout)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        val replayed = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        isRenowned(driver, replayed) shouldBe false
        plusOneCounters(driver, replayed) shouldBe 0
    }

    test("the projected keyword gates the trigger — losing all abilities loses renown") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(player, "Test Renowned Scout")
        driver.removeSummoningSickness(scout)

        // "This creature loses all abilities" — the ability layer, which strips the projected
        // keyword the derivation gates on.
        driver.replaceState(
            driver.state.addFloatingEffects(
                listOf(
                    ActiveFloatingEffect(
                        id = EntityId.generate(),
                        effect = FloatingEffectData(
                            layer = Layer.ABILITY,
                            modification = SerializableModification.RemoveAllAbilities,
                            affectedEntities = setOf(scout),
                        ),
                        duration = Duration.Permanent,
                        sourceId = scout,
                        sourceName = "Test Renowned Scout",
                        controllerId = player,
                        timestamp = driver.state.timestamp,
                    )
                )
            )
        )
        driver.state.projectedState.hasKeyword(scout, Keyword.RENOWN) shouldBe false

        attackUnblocked(driver, player, listOf(scout))

        plusOneCounters(driver, scout) shouldBe 0
        isRenowned(driver, scout) shouldBe false
    }

    test("a payoff reads the designation back — renowned grants menace (Goblin Glory Chaser shape)") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val chaser = driver.putCreatureOnBattlefield(player, "Test Glory Chaser")
        driver.removeSummoningSickness(chaser)

        driver.state.projectedState.hasKeyword(chaser, Keyword.MENACE) shouldBe false

        attackUnblocked(driver, player, listOf(chaser))

        isRenowned(driver, chaser) shouldBe true
        driver.state.projectedState.hasKeyword(chaser, Keyword.MENACE) shouldBe true
    }

    // ---- Granted renown (Aragorn, Hornburg Hero) -------------------------------------------------

    test("granted renown — an attacker with no printed renown triggers, gets 1 counter and is renowned") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Not attacking yet: the grant's group filter doesn't cover it.
        driver.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false

        val triggers = attackUnblocked(driver, player, listOf(bears)) {
            driver.state.projectedState.getKeywords(bears).contains(KeywordAbility.grantedRenown(1)) shouldBe true
            driver.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe true
        }

        triggers shouldBe 1
        plusOneCounters(driver, bears) shouldBe 1
        isRenowned(driver, bears) shouldBe true
        // Combat is over, so it no longer has the granted keyword — but stays renowned.
        driver.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false
    }

    test("CR 702.112c — printed renown 2 plus granted renown 1 are two triggers, and only the first adds counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        val captain = driver.putCreatureOnBattlefield(player, "Test Renowned Captain")
        driver.removeSummoningSickness(captain)

        val triggers = attackUnblocked(driver, player, listOf(captain))

        // Two separate abilities, not one fused renown 3.
        triggers shouldBe 2
        (plusOneCounters(driver, captain) in 1..2) shouldBe true
        isRenowned(driver, captain) shouldBe true
    }

    test("CR 702.112c — two identical renown 1 grants are two triggers, not one") {
        // Two Heralds each grant renown 1. The projected keyword set holds one `RENOWN_1`, so the
        // grant tally is what keeps the second instance.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        val triggers = attackUnblocked(driver, player, listOf(bears)) {
            driver.state.projectedState.grantedKeywordInstances(bears, KeywordAbility.grantedRenown(1)) shouldBe 2
        }

        triggers shouldBe 2
        plusOneCounters(driver, bears) shouldBe 1
        isRenowned(driver, bears) shouldBe true
    }

    test("granted renown ends when the grant's source leaves the battlefield") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val herald = driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        val triggers = attackUnblocked(driver, player, listOf(bears)) {
            driver.moveToGraveyard(herald)
            driver.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false
        }

        triggers shouldBe 0
        plusOneCounters(driver, bears) shouldBe 0
        isRenowned(driver, bears) shouldBe false
    }

    test("the grant is a static ability — a Herald that loses all abilities grants no renown") {
        // CR 604.2: a static ability's effect exists only while its source has the ability.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val herald = driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        stamp(driver, herald, 100)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        val triggers = attackUnblocked(driver, player, listOf(bears)) {
            loseAllAbilities(driver, herald, player, timestamp = 101)
            driver.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false
        }

        triggers shouldBe 0
        plusOneCounters(driver, bears) shouldBe 0
        isRenowned(driver, bears) shouldBe false
    }

    test("a creature that lost its printed renown but is granted renown 1 gets only the granted trigger") {
        // The printed half gates on the bare RENOWN keyword alone; the granted RENOWN_1 must not
        // revive the printed renown 2 the creature lost.
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val captain = driver.putCreatureOnBattlefield(player, "Test Renowned Captain")
        driver.removeSummoningSickness(captain)
        loseAllAbilities(driver, captain, player, timestamp = 100)
        // The Herald's grant is the later layer-6 effect, so it applies after the removal.
        val herald = driver.putCreatureOnBattlefield(player, "Test Renown Herald")
        stamp(driver, herald, 101)

        val triggers = attackUnblocked(driver, player, listOf(captain))

        triggers shouldBe 1
        plusOneCounters(driver, captain) shouldBe 1
        isRenowned(driver, captain) shouldBe true
    }
})
