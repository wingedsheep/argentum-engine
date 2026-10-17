package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.mechanics.combat.CombatTaxes
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * The combat rules that treat a battle differently from a planeswalker: its defending player is its
 * **protector**, not its controller (CR 310.9d / 508.5). A Siege is controlled by the player who
 * cast it and protected by an opponent, so every place that reads "the defending player" off the
 * attacked permanent's controller gets a Siege backwards — it names the attacker.
 *
 * Every board here is the Siege shape: Player (seat 1) controls the battle, Opponent (seat 2)
 * protects it, and Player's creatures attack it.
 */
class BattleCombatRulesScenarioTest : ScenarioTestBase() {

    private val testSiege = card("Test Rampart") {
        manaCost = "{2}{W}"
        colorIdentity = "W"
        typeLine = "Battle — Siege"
        startingDefense = 5
        oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack it.)"
    }

    /** A permanent that is both a creature and a battle — CR 506.3f says it can't attack or block. */
    private val testBattleCreature = card("Test Siege Golem") {
        manaCost = "{3}"
        colorIdentity = ""
        typeLine = "Artifact Creature Battle — Golem Siege"
        power = 3
        toughness = 3
        startingDefense = 3
        oracleText = "A creature that is also a battle."
    }

    private fun defenseOf(game: TestGame, name: String): Int =
        game.findPermanent(name)
            ?.let { game.state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) }
            ?: 0

    /** Player controls [siege] (protected by Opponent once SBAs run) plus [extra] permanents. */
    private fun siegeBoard(
        extraPlayer: List<String> = emptyList(),
        extraOpponent: List<String> = emptyList(),
        phase: Phase = Phase.PRECOMBAT_MAIN,
        step: Step = Step.PRECOMBAT_MAIN,
        configure: (ScenarioBuilder) -> Unit = {},
    ): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Rampart")
            .withActivePlayer(1)
            .inPhase(phase, step)
        extraPlayer.forEach { builder.withCardOnBattlefield(1, it, summoningSickness = false) }
        extraOpponent.forEach { builder.withCardOnBattlefield(2, it, summoningSickness = false) }
        configure(builder)
        val game = builder.build()
        game.checkStateBasedActions()
        withClue("setup: the Siege's protector is the opponent (CR 310.12a)") {
            Battles.protectorOf(game.state, game.findPermanent("Test Rampart")!!) shouldBe game.player2Id
        }
        return game
    }

    init {
        cardRegistry.register(testSiege)
        cardRegistry.register(testBattleCreature)

        context("CR 310.9d / 508.5 — the defending player of an attack on a battle is its protector") {

            test("'defending player loses 1 life' hits the protector when you attack your own Siege") {
                val game = siegeBoard(extraPlayer = listOf("Agate-Blade Assassin"))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Agate-Blade Assassin" to "Test Rampart")
                ).error shouldBe null
                game.resolveStack()

                withClue("the protector (Opponent) is the defending player and loses 1") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("the Siege's controller (Player) only gains the 1 — they are not defending") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }
        }

        context("CR 310.12b / 704.5v — a Siege defeated in combat alongside another death") {

            test("a blocker dying in the same damage step doesn't cost the Siege its defeat trigger") {
                val game = siegeBoard(
                    extraPlayer = listOf("Serra Angel", "Hill Giant"),
                    extraOpponent = listOf("Grizzly Bears"),
                )
                // Chip the Siege to 3 so the Angel's 4 finishes it.
                val siegeId = game.findPermanent("Test Rampart")!!
                game.state = game.state.updateEntity(siegeId) {
                    it.with(CountersComponent().withAdded(CounterType.DEFENSE, 3))
                }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Serra Angel" to "Test Rampart", "Hill Giant" to "Test Rampart")
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Hill Giant killed its blocker in the same combat damage step") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("CR 704.5v — the Siege was spared for its defeat trigger, which exiled it") {
                    game.isInExile(1, "Test Rampart") shouldBe true
                    game.isInGraveyard(1, "Test Rampart") shouldBe false
                }
            }
        }

        context("Attack taxes name 'you' (and sometimes planeswalkers) — never a battle") {

            test("your own Ghostly Prison doesn't tax your attack on your own Siege") {
                val game = siegeBoard(extraPlayer = listOf("Ghostly Prison", "Grizzly Bears"))
                val attackers = mapOf(game.findPermanent("Grizzly Bears")!! to game.findPermanent("Test Rampart")!!)
                withClue("the Siege's controller is not the player being attacked") {
                    CombatTaxes.attackTax(game.state, cardRegistry, attackers, game.state.projectedState, predicateEvaluator = services.predicateEvaluator) shouldBe 0
                }
            }

            test("the protector's Ghostly Prison taxes attacks on them, not on the battle they protect") {
                val game = siegeBoard(extraPlayer = listOf("Grizzly Bears"), extraOpponent = listOf("Ghostly Prison"))
                val bears = game.findPermanent("Grizzly Bears")!!
                val onBattle = mapOf(bears to game.findPermanent("Test Rampart")!!)
                val onPlayer = mapOf(bears to game.player2Id)
                withClue("'creatures can't attack you' — a battle is not 'you'") {
                    CombatTaxes.attackTax(game.state, cardRegistry, onBattle, game.state.projectedState, predicateEvaluator = services.predicateEvaluator) shouldBe 0
                }
                withClue("attacking the protector themself pays {2}") {
                    CombatTaxes.attackTax(game.state, cardRegistry, onPlayer, game.state.projectedState, predicateEvaluator = services.predicateEvaluator) shouldBe 2
                }
            }

            test("a tax that says 'or planeswalkers you control' covers them; one that doesn't, doesn't") {
                val game = siegeBoard(
                    extraPlayer = listOf("Grizzly Bears"),
                    extraOpponent = listOf("Ghostly Prison", "Archangel of Tithes", "Ajani Goldmane"),
                )
                val attackers = mapOf(game.findPermanent("Grizzly Bears")!! to game.findPermanent("Ajani Goldmane")!!)
                withClue("Archangel of Tithes' {1} applies to a planeswalker; Ghostly Prison's {2} does not") {
                    CombatTaxes.attackTax(game.state, cardRegistry, attackers, game.state.projectedState, predicateEvaluator = services.predicateEvaluator) shouldBe 1
                }
            }
        }

        context("CR 506.4 — a battle whose controller changes is removed from combat") {

            test("its attackers keep attacking but deal no combat damage (CR 506.4c, 510.1b)") {
                val game = siegeBoard(extraPlayer = listOf("Serra Angel"))
                val siegeId = game.findPermanent("Test Rampart")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Serra Angel" to "Test Rampart")
                ).error shouldBe null

                // The protector gains control of the Siege mid-combat. CR 704.5y then hands
                // protection back to Player (the only opponent of its new controller).
                game.state = game.state.updateEntity(siegeId) { it.with(ControllerComponent(game.player2Id)) }
                game.checkStateBasedActions()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Angel dealt nothing to the battle removed from combat") {
                    defenseOf(game, "Test Rampart") shouldBe 5
                }
                withClue("nor to any player") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("an attacked planeswalker that leaves the battlefield takes its unblocked attackers' damage with it") {
                val game = siegeBoard(extraPlayer = listOf("Serra Angel"), extraOpponent = listOf("Ajani Goldmane"))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Serra Angel" to "Ajani Goldmane")
                ).error shouldBe null
                val ajani = game.findPermanent("Ajani Goldmane")!!
                game.state = game.state.updateEntity(ajani) { it.with(CountersComponent()) }
                game.checkStateBasedActions()
                game.isOnBattlefield("Ajani Goldmane") shouldBe false
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("CR 510.1b — an unblocked creature attacking nothing assigns no combat damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("CR 506.3f / 508.1a / 509.1a — a creature that is also a battle can't attack or block") {

            test("it can't be declared as an attacker") {
                val game = siegeBoard(extraPlayer = listOf("Test Siege Golem"))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val golem = game.findPermanent("Test Siege Golem")!!
                val result = game.execute(DeclareAttackers(game.player1Id, mapOf(golem to game.player2Id)))
                result.error shouldNotBe null
                result.error!! shouldContain "battle can't attack"
            }

            test("it can't be declared as a blocker") {
                val game = siegeBoard(extraPlayer = listOf("Grizzly Bears"), extraOpponent = listOf("Test Siege Golem"))
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val golem = game.findPermanent("Test Siege Golem")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.execute(DeclareBlockers(game.player2Id, mapOf(golem to listOf(bears))))
                result.error shouldNotBe null
                result.error!! shouldContain "battle can't block"
            }
        }

        context("CR 310.10 / 704.5p — a battle can't be attached to anything") {

            test("an attached battle becomes unattached and stays on the battlefield") {
                val game = siegeBoard(extraPlayer = listOf("Grizzly Bears"))
                val siegeId = game.findPermanent("Test Rampart")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(siegeId) { it.with(AttachedToComponent(bears)) }
                game.checkStateBasedActions()

                game.state.getEntity(siegeId)?.has<AttachedToComponent>() shouldBe false
                game.isOnBattlefield("Test Rampart") shouldBe true
            }
        }

        context("CR 508.4a / 702.49c — ninjutsu into an attack on a battle") {

            test("the ninja enters attacking the battle the returned creature was attacking") {
                val game = siegeBoard(extraPlayer = listOf("Grizzly Bears"), phase = Phase.PRECOMBAT_MAIN) { b ->
                    b.withCardInHand(1, "Azra Smokeshaper")
                    b.withLandsOnBattlefield(1, "Swamp", 2)
                }
                val siegeId = game.findPermanent("Test Rampart")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Test Rampart")
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                var guard = 0
                while (game.state.priorityPlayerId != game.player1Id &&
                    game.state.step == Step.DECLARE_BLOCKERS && guard++ < 4
                ) {
                    game.passPriority()
                }

                val ninjaCard = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Azra Smokeshaper"
                }
                val cast = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ninjaCard,
                        abilityId = ninjutsuAbility("{1}{B}").id,
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(bears)),
                    )
                )
                withClue("ninjutsu: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val ninja = game.findPermanent("Azra Smokeshaper")!!
                withClue("it attacks the battle, not nothing") {
                    game.state.getEntity(ninja)?.get<AttackingComponent>()?.defenderId shouldBe siegeId
                }
            }
        }
    }
}
