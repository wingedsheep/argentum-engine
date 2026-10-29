package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.captureLastKnown
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The scope axes of the combat-damage batch triggers, and "players being attacked":
 *
 * - `OneOrMoreDealCombatDamageToPlayerEvent.toOneOrMorePlayers` — "… deal combat damage to one or
 *   more players" fires once per combat-damage batch however many players were hit (the default
 *   fires once per damaged player). Each combat damage step is its own batch (CR 510.4: a
 *   first-strike step, then a second combat damage step).
 * - `OneOrMoreDealCombatDamageToPlayerEvent.toOpponents` — only the observer's opponents count as
 *   damaged players, in both the per-player and the batch form; the batch form records the players
 *   it hit (`DynamicAmounts.playersDealtDamageThisWay()`).
 * - `OneOrMoreDealCombatDamageToYouEvent.perSourceController` — "… creatures an opponent controls
 *   deal combat damage to you, that opponent …" fires once per controller of the damaging
 *   creatures, binding that player; the default (Witch-king of Angmar) stays one unbound trigger.
 * - `Player.EachAttackedPlayer` — the players some creature is attacking directly. A player whose
 *   planeswalker alone is attacked is not "being attacked" (CR 802.4 lists the two separately),
 *   nor is an opponent nothing attacks. "Whenever a player attacks" (CR 508.3d) binds the attacking
 *   player as the triggering player.
 */
class CombatDamageBatchScopeTest : FunSpec({

    val toOneOrMorePlayersWatcher = card("Test Spreading Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToOneOrMorePlayers()
            effect = Effects.GainLife(1)
        }
    }

    val perControllerWatcher = card("Test Retribution Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature.opponentControls())
                .dealCombatDamageToYou(perSourceController = true)
            effect = Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    val defaultToYouWatcher = card("Test Defensive Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToYou()
            effect = Effects.GainLife(1)
        }
    }

    val attackedPoisonedWatcher = card("Test Attacked Poisoned Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.anyPlayer.attacks()
            interveningIf = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.playerCounterCount(CounterType.POISON),
                    players = Player.EachAttackedPlayer,
                ),
                ComparisonOperator.GTE,
                1,
            )
            effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    val firstStriker = card("Test First Striker") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        keywords(Keyword.FIRST_STRIKE)
    }

    val walker = card("Test Attacked Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val cards = TestCards.all + listOf(
        toOneOrMorePlayersWatcher, perControllerWatcher, defaultToYouWatcher, attackedPoisonedWatcher,
        firstStriker, walker,
    )

    fun detector(driver: GameTestDriver) = TriggerDetector(
        driver.cardRegistry,
        conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions,
        predicateEvaluator = PredicateEvaluator(cardRegistry = null),
    )

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(cards)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun combatHit(source: EntityId, target: EntityId, lastKnown: com.wingedsheep.engine.state.components.stack.EntitySnapshot? = null) =
        DamageDealtEvent(source, target, 1, isCombatDamage = true, targetIsPlayer = true, sourceLastKnown = lastKnown)

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun GameTestDriver.poison(player: EntityId, count: Int) {
        replaceState(state.updateEntity(player) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) })
    }

    fun GameTestDriver.poisonOf(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    context("toOpponents — \"… to an opponent\" / \"… to one or more of your opponents\"") {

        fun watcher(name: String, opponentsOnlyBatch: Boolean) = card(name) {
            manaCost = "{1}"
            typeLine = "Enchantment"
            triggeredAbility {
                val creatures = Triggers.oneOrMore(GameObjectFilter.Creature)
                trigger = if (opponentsOnlyBatch) creatures.dealCombatDamageToOneOrMoreOpponents()
                else creatures.dealCombatDamageToAnOpponent()
                effect = Effects.GainLife(1)
            }
        }

        test("the batch form fires once and records only the opponents hit") {
            val (d, p) = threePlayer()
            d.registerCards(listOf(watcher("Test Opponents Batch Watcher", opponentsOnlyBatch = true)))
            d.putPermanentOnBattlefield(p[0], "Test Opponents Batch Watcher")
            val a = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")
            val b = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")
            val c = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")

            val fired = detector(d).detectTriggers(
                d.state, listOf(combatHit(a, p[1]), combatHit(b, p[2]), combatHit(c, p[0]))
            ).filter { it.ability.trigger is EventPattern.OneOrMoreDealCombatDamageToPlayerEvent }

            fired shouldHaveSize 1
            fired[0].triggerContext.damagedPlayerIds.orEmpty().shouldContainExactlyInAnyOrder(p[1], p[2])
            withClue("the creature that hit its own controller isn't one of them") {
                fired[0].triggerContext.capturedEntityIds.orEmpty().shouldContainExactlyInAnyOrder(a, b)
            }
        }

        test("damage only to a non-opponent doesn't fire either form") {
            val (d, p) = threePlayer()
            d.registerCards(
                listOf(
                    watcher("Test Opponents Batch Watcher", opponentsOnlyBatch = true),
                    watcher("Test An Opponent Watcher", opponentsOnlyBatch = false),
                )
            )
            d.putPermanentOnBattlefield(p[0], "Test Opponents Batch Watcher")
            d.putPermanentOnBattlefield(p[0], "Test An Opponent Watcher")
            val a = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")

            detector(d).detectTriggers(d.state, listOf(combatHit(a, p[0]))).shouldBeEmpty()
        }

        test("the per-opponent form fires once per opponent hit, binding that opponent") {
            val (d, p) = threePlayer()
            d.registerCards(listOf(watcher("Test An Opponent Watcher", opponentsOnlyBatch = false)))
            d.putPermanentOnBattlefield(p[0], "Test An Opponent Watcher")
            val a = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")
            val b = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")

            val fired = detector(d).detectTriggers(
                d.state, listOf(combatHit(a, p[1]), combatHit(b, p[2]), combatHit(b, p[0]))
            )
            fired.map { it.triggerContext.triggeringPlayerId }.shouldContainExactlyInAnyOrder(p[1], p[2])
        }

        test("the damaged-player count reaches the resolving ability") {
            val d = GameTestDriver()
            val counter = card("Test Damaged Count Watcher") {
                manaCost = "{1}"
                typeLine = "Enchantment"
                triggeredAbility {
                    trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToOneOrMoreOpponents()
                    effect = Effects.GainLife(DynamicAmounts.playersDealtDamageThisWay())
                }
            }
            d.registerCards(cards + counter)
            val p = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.putPermanentOnBattlefield(p[0], "Test Damaged Count Watcher")
            val a = d.ready(p[0], "Grizzly Bears")
            val b = d.ready(p[0], "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(a to p[1], b to p[2])).error shouldBe null
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            withClue("two opponents dealt damage this way") { d.getLifeTotal(p[0]) shouldBe 22 }
        }
    }

    context("toOneOrMorePlayers — \"deal combat damage to one or more players\"") {

        test("fires once for creatures hitting two different players, capturing both sources") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Spreading Watcher")
            val a = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")
            val b = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")

            val fired = detector(d).detectTriggers(d.state, listOf(combatHit(a, p[1]), combatHit(b, p[2])))
                .filter { it.ability.trigger is EventPattern.OneOrMoreDealCombatDamageToPlayerEvent }

            fired shouldHaveSize 1
            fired[0].controllerId shouldBe p[0]
            withClue("no single damaged player to bind") { fired[0].triggerContext.triggeringPlayerId shouldBe null }
            fired[0].triggerContext.capturedEntityIds.shouldContainExactlyInAnyOrder(a, b)
        }

        test("the default \"to a player\" form still fires once per damaged player") {
            val (d, p) = threePlayer()
            val kastralShape = card("Test Per Player Watcher") {
                manaCost = "{1}"
                typeLine = "Enchantment"
                triggeredAbility {
                    trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
                    effect = Effects.GainLife(1)
                }
            }
            d.registerCards(listOf(kastralShape))
            d.putPermanentOnBattlefield(p[0], "Test Per Player Watcher")
            val a = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")
            val b = d.putCreatureOnBattlefield(p[0], "Grizzly Bears")

            val fired = detector(d).detectTriggers(d.state, listOf(combatHit(a, p[1]), combatHit(b, p[2])))
            fired.map { it.triggerContext.triggeringPlayerId }.shouldContainExactlyInAnyOrder(p[1], p[2])
        }

        test("only the controller's creatures count") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Spreading Watcher")
            val theirs = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")

            detector(d).detectTriggers(d.state, listOf(combatHit(theirs, p[2]))).shouldBeEmpty()
        }

        test("each combat damage step is its own batch: first strike, then regular damage") {
            val d = GameTestDriver()
            d.registerCards(cards)
            d.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val me = d.activePlayer!!
            val opp = d.getOpponent(me)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.putPermanentOnBattlefield(me, "Test Spreading Watcher")
            val striker = d.ready(me, "Test First Striker")
            val bears = d.ready(me, "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(me, listOf(striker, bears), opp).error shouldBe null
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            d.getLifeTotal(opp) shouldBe 17
            withClue("one trigger in the first-strike step, one in the regular step") { d.getLifeTotal(me) shouldBe 22 }
        }

        test("two creatures in one combat damage step fire it once") {
            val d = GameTestDriver()
            d.registerCards(cards)
            d.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val me = d.activePlayer!!
            val opp = d.getOpponent(me)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.putPermanentOnBattlefield(me, "Test Spreading Watcher")
            val a = d.ready(me, "Grizzly Bears")
            val b = d.ready(me, "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(me, listOf(a, b), opp).error shouldBe null
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            d.getLifeTotal(opp) shouldBe 16
            d.getLifeTotal(me) shouldBe 21
        }
    }

    context("perSourceController — \"creatures an opponent controls deal combat damage to you, that opponent …\"") {

        test("fires once per opponent whose creatures hit you, binding that opponent") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Retribution Watcher")
            val fromP2a = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")
            val fromP2b = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")
            val fromP3 = d.putCreatureOnBattlefield(p[2], "Grizzly Bears")

            val fired = detector(d).detectTriggers(
                d.state, listOf(combatHit(fromP2a, p[0]), combatHit(fromP2b, p[0]), combatHit(fromP3, p[0]))
            ).filter { it.ability.trigger is EventPattern.OneOrMoreDealCombatDamageToYouEvent }

            fired shouldHaveSize 2
            fired.forEach { it.controllerId shouldBe p[0] }
            val byPlayer = fired.associateBy { it.triggerContext.triggeringPlayerId }
            byPlayer.keys shouldBe setOf(p[1], p[2])
            byPlayer.getValue(p[1]).triggerContext.capturedEntityIds.shouldContainExactlyInAnyOrder(fromP2a, fromP2b)
            byPlayer.getValue(p[2]).triggerContext.capturedEntityIds.shouldContainExactly(fromP3)
        }

        test("a creature killed by the same damage still counts for its last-known controller") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Retribution Watcher")
            val bears = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")
            val lastKnown = captureLastKnown(d.state, bears)
            d.moveToGraveyard(bears)

            val fired = detector(d).detectTriggers(d.state, listOf(combatHit(bears, p[0], lastKnown)))
            fired shouldHaveSize 1
            fired[0].triggerContext.triggeringPlayerId shouldBe p[1]
        }

        test("damage dealt to another player doesn't fire it") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Retribution Watcher")
            val bears = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")

            detector(d).detectTriggers(d.state, listOf(combatHit(bears, p[2]))).shouldBeEmpty()
        }

        test("the default form is unchanged: one trigger per batch, no triggering player") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Defensive Watcher")
            val fromP2 = d.putCreatureOnBattlefield(p[1], "Grizzly Bears")
            val fromP3 = d.putCreatureOnBattlefield(p[2], "Grizzly Bears")

            val fired = detector(d).detectTriggers(d.state, listOf(combatHit(fromP2, p[0]), combatHit(fromP3, p[0])))
            fired shouldHaveSize 1
            fired[0].triggerContext.triggeringPlayerId shouldBe null
            fired[0].triggerContext.triggeringEntityId shouldBe fromP2
        }

        test("in a real combat the attacking opponent gets the poison counter") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[1], "Test Retribution Watcher")
            val a = d.ready(p[0], "Grizzly Bears")
            val b = d.ready(p[0], "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(a to p[1], b to p[1])).error shouldBe null
            d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            d.getLifeTotal(p[1]) shouldBe 16
            d.poisonOf(p[0]) shouldBe 1
            d.poisonOf(p[1]) shouldBe 0
            d.poisonOf(p[2]) shouldBe 0
        }
    }

    context("Player.EachAttackedPlayer and \"whenever a player attacks\"") {

        test("attacking a poisoned player: the attacking player draws") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[1], "Test Attacked Poisoned Watcher")
            d.poison(p[1], 1)
            val bears = d.ready(p[0], "Grizzly Bears")
            val hand = d.getHandSize(p[0])

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(bears to p[1])).error shouldBe null
            CombatDefenders.attackedPlayersInApnapOrder(d.state) shouldContainExactly listOf(p[1])
            d.stackSize shouldBe 1
            d.resolveAll()

            withClue("the attacking player — not the watcher's controller — draws") {
                d.getHandSize(p[0]) shouldBe hand + 1
            }
        }

        test("a poisoned defending player nothing attacks doesn't count") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Attacked Poisoned Watcher")
            d.poison(p[1], 3)
            val bears = d.ready(p[0], "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(bears to p[2])).error shouldBe null
            withClue("p2 is a defending seat but isn't being attacked") {
                CombatDefenders.allDefendingPlayersInApnapOrder(d.state).contains(p[1]) shouldBe true
                CombatDefenders.attackedPlayersInApnapOrder(d.state) shouldContainExactly listOf(p[2])
            }
            d.stackSize shouldBe 0
        }

        test("attacking only a poisoned player's planeswalker doesn't make that player attacked") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Attacked Poisoned Watcher")
            d.poison(p[1], 2)
            val pw = d.putPermanentOnBattlefield(p[1], "Test Attacked Walker")
            d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
            val bears = d.ready(p[0], "Grizzly Bears")

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(bears to pw)).error shouldBe null
            CombatDefenders.attackedPlayersInApnapOrder(d.state).shouldBeEmpty()
            d.stackSize shouldBe 0
        }

        test("the intervening-if is rechecked on resolution: the attacker leaving combat stops the draw") {
            val (d, p) = threePlayer()
            d.putPermanentOnBattlefield(p[0], "Test Attacked Poisoned Watcher")
            d.poison(p[1], 1)
            val bears = d.ready(p[0], "Grizzly Bears")
            val hand = d.getHandSize(p[0])

            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(p[0], mapOf(bears to p[1])).error shouldBe null
            d.stackSize shouldBe 1
            // Removed from combat in response: p2 is no longer being attacked.
            d.replaceState(d.state.updateEntity(bears) { it.without<AttackingComponent>() })
            d.resolveAll()

            d.getHandSize(p[0]) shouldBe hand
        }

        test("nobody is being attacked outside combat") {
            val (d, _) = threePlayer()
            CombatDefenders.attackedPlayersInApnapOrder(d.state).shouldBeEmpty()
        }
    }
})
