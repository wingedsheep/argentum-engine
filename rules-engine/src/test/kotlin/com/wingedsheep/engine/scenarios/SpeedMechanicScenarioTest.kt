package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for **speed** — "Start your engines!" (CR 702.179) and "Max speed" (CR 702.178),
 * Aetherdrift.
 *
 * One test per clause of the rules, because every clause is load-bearing somewhere:
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 704.5z / 702.179a | controlling a permanent with the keyword and having no speed ⇒ speed becomes 1 | "starts a controller's speed at 1", "gaining control", "granted keyword" |
 * | 702.179b | players have no speed until something sets it | "a player with no speed-granting permanent never gains speed" |
 * | 702.179c | increasing a no-speed player's speed sets it to that amount | "increasing from no speed lands on the amount" |
 * | 702.179d | the inherent trigger: opponents lose life on your turn, speed < 4, +1, once each turn | "rises when an opponent loses life", "only once each turn", "not on an opponent's turn" |
 * | 702.179e | max speed is a speed of *exactly* 4 | "max speed gates …" tests, "caps at 4" |
 * | 702.179f | an effect reading the speed of a player with none sees 0 | "reads as 0 for a player with no speed" |
 * | 702.178a | "Max speed — [Ability]" ⇒ the object has [Ability] while your speed is 4 | the three "max speed gates" tests |
 */
class SpeedMechanicScenarioTest : ScenarioTestBase() {

    // ── Test cards ───────────────────────────────────────────────────────────────
    // Each isolates one shape of the mechanic; behaviour under test is the engine's, not theirs.

    /** Bare "Start your engines!" — nothing but the keyword, so the SBA is the only thing acting. */
    private val engineStarter = card("Test Engine Starter") {
        manaCost = "{1}"
        typeLine = "Creature — Human Pilot"
        power = 1
        toughness = 1
        startYourEngines()
    }

    /** No speed keyword at all — the control for CR 702.179b. */
    private val plainBear = card("Test Plain Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    /** "Max speed — This creature has double strike." (Burnout Bashtronaut's shape.) */
    private val maxSpeedKeyword = card("Test Max Speed Keyword") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 2
        toughness = 1
        startYourEngines()
        maxSpeed { keywords(Keyword.DOUBLE_STRIKE) }
    }

    /** "Max speed — {T}: You gain 2 life." (Raceway shape: an activated ability behind the gate.) */
    private val maxSpeedActivated = card("Test Max Speed Activated") {
        manaCost = "{2}"
        typeLine = "Artifact"
        startYourEngines()
        maxSpeed {
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.GainLife(2)
            }
        }
    }

    /** "Max speed — Whenever this creature attacks, you gain 3 life." (Triggered behind the gate.) */
    private val maxSpeedTriggered = card("Test Max Speed Triggered") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Cat Scout"
        power = 2
        toughness = 2
        startYourEngines()
        maxSpeed {
            triggeredAbility {
                trigger = Triggers.Attacks
                effect = Effects.GainLife(3)
            }
        }
    }

    /** "This creature gets +X/+0, where X is your speed." — proves DynamicAmount.Speed in projection. */
    private val speedScaler = card("Test Speed Scaler") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Elephant"
        power = 1
        toughness = 1
        staticAbility {
            ability = GrantDynamicStatsEffect(
                filter = GroupFilter.source(),
                powerBonus = DynamicAmounts.speed(Player.You),
                toughnessBonus = DynamicAmount.Fixed(0)
            )
        }
    }

    /** Sorcery: each opponent loses 1 life — drives the inherent speed trigger. */
    private val drainOne = card("Test Drain One") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    /** Sorcery: you gain life equal to your speed — proves speed is readable at resolution. */
    private val speedSiphon = card("Test Speed Siphon") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(DynamicAmounts.speed(Player.You)) }
    }

    /** Sorcery: gain control of target creature — drives the change-of-control SBA test. */
    private val steal = card("Test Steal") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.GainControl(t)
        }
    }

    /** Sorcery: destroy target creature — proves speed outlives the permanent that started it. */
    private val slay = card("Test Slay") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.Destroy(t)
        }
    }

    /**
     * Sorcery: reduce target opponent's speed by 1, but not below 1 — Spikeshell Harrier's rider,
     * the one printed effect that lowers speed.
     */
    private val slowDown = card("Test Slow Down") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ReduceSpeed(target = EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    /** Grants "Start your engines!" to another creature — proves the SBA reads projected keywords. */
    private val engineGranter = card("Test Engine Granter") {
        manaCost = "{1}{U}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantKeyword(
                Keyword.START_YOUR_ENGINES,
                GroupFilter.AllCreaturesYouControl
            )
        }
    }

    init {
        listOf(
            engineStarter, plainBear, maxSpeedKeyword, maxSpeedActivated, maxSpeedTriggered,
            speedScaler, drainOne, speedSiphon, engineGranter, steal, slay, slowDown
        ).forEach { cardRegistry.register(it) }

        context("Start your engines! (CR 702.179a / 704.5z)") {

            test("starts a controller's speed at 1 as a state-based action") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                }

                withClue("Controlling a permanent with the keyword sets speed to 1") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
                withClue("The opponent controls no such permanent, so they still have no speed") {
                    game.state.hasSpeed(game.player2Id) shouldBe false
                    game.state.speed(game.player2Id) shouldBe Speed.NONE
                }
            }

            test("a player with no speed-granting permanent never gains speed (CR 702.179b)") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Plain Bear", summoningSickness = false)
                }

                withClue("No keyword anywhere means nobody has speed") {
                    game.state.hasSpeed(game.player1Id) shouldBe false
                    game.state.hasSpeed(game.player2Id) shouldBe false
                }
            }

            test("several permanents with the keyword still start exactly one speed of 1") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardOnBattlefield(1, "Test Max Speed Keyword", summoningSickness = false)
                }

                withClue("The SBA is per-player, not per-permanent") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
            }

            test("gaining control of an opponent's permanent with the keyword starts your speed") {
                val game = speedGame {
                    withCardOnBattlefield(2, "Test Engine Starter", summoningSickness = false)
                    withCardInHand(1, "Test Steal")
                    withLandsOnBattlefield(1, "Island", 1)
                }
                withClue("Its own controller's speed starts first") {
                    game.state.speed(game.player2Id) shouldBe Speed.STARTING
                    game.state.hasSpeed(game.player1Id) shouldBe false
                }

                val starter = game.findPermanent("Test Engine Starter")!!
                val steal = game.castSpell(1, "Test Steal", targetId = starter)
                withClue("Stealing the permanent should succeed: ${steal.error}") {
                    steal.error shouldBe null
                }
                game.resolveStack()

                withClue("The new controller's speed starts too — no trigger needed, it's an SBA") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
                withClue("Speed is never lost, so the old controller keeps theirs (CR 702.179)") {
                    game.state.speed(game.player2Id) shouldBe Speed.STARTING
                }
            }

            test("a permanent granted the keyword starts its controller's speed (projected state)") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Plain Bear", summoningSickness = false)
                    withCardOnBattlefield(1, "Test Engine Granter", summoningSickness = false)
                }

                withClue("The SBA reads projected keywords, so a Layer 6 grant counts") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
            }

            test("speed survives the granting permanent leaving the battlefield") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardInHand(1, "Test Slay")
                    withLandsOnBattlefield(1, "Swamp", 1)
                }
                game.state.speed(game.player1Id) shouldBe Speed.STARTING

                val starter = game.findPermanent("Test Engine Starter")!!
                val slain = game.castSpell(1, "Test Slay", targetId = starter)
                withClue("Destroying the starter should succeed: ${slain.error}") {
                    slain.error shouldBe null
                }
                game.resolveStack()
                withClue("The permanent really left the battlefield") {
                    game.isOnBattlefield("Test Engine Starter") shouldBe false
                }

                withClue("Nothing ever removes speed once gained") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
            }
        }

        context("The inherent speed trigger (CR 702.179d)") {

            test("speed rises by 1 when an opponent loses life during your turn") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardInHand(1, "Test Drain One")
                    withLandsOnBattlefield(1, "Swamp", 1)
                }
                game.state.speed(game.player1Id) shouldBe Speed.STARTING

                game.castSpell(1, "Test Drain One")
                game.resolveStack()

                withClue("One opponent losing life on your turn raises your speed to 2") {
                    game.state.speed(game.player1Id) shouldBe 2
                }
                withClue("The opponent has no speed, so they have no inherent trigger at all") {
                    game.state.hasSpeed(game.player2Id) shouldBe false
                }
            }

            test("triggers only once each turn, no matter how often opponents lose life") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardsInHand(1, "Test Drain One", 3)
                    withLandsOnBattlefield(1, "Swamp", 3)
                }

                repeat(3) {
                    game.castSpell(1, "Test Drain One")
                    game.resolveStack()
                }

                withClue("Three separate life-loss events in one turn still only give +1") {
                    game.state.speed(game.player1Id) shouldBe 2
                }
                withClue("The drains really did resolve") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("does not trigger when an opponent loses life on someone else's turn") {
                val game = speedGame(activePlayer = 2) {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardInHand(2, "Test Drain One")
                    withLandsOnBattlefield(2, "Swamp", 1)
                }
                game.state.speed(game.player1Id) shouldBe Speed.STARTING

                // Player 2 drains player 1 on player 2's own turn: player 1 (the speed holder) loses
                // life, and it isn't their turn — neither half of the trigger's condition is met.
                game.castSpell(2, "Test Drain One")
                game.resolveStack()

                withClue("\"during your turn\" gates the trigger to the speed holder's own turn") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
            }

            test("the once-each-turn cap resets, so speed can rise again next turn") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardsInHand(1, "Test Drain One", 2)
                    withLandsOnBattlefield(1, "Swamp", 2)
                }

                game.castSpell(1, "Test Drain One")
                game.resolveStack()
                game.state.speed(game.player1Id) shouldBe 2

                game.roundTheTableToPlayerOne()

                game.castSpell(1, "Test Drain One")
                game.resolveStack()

                withClue("A fresh turn means a fresh trigger, so speed reaches 3") {
                    game.state.speed(game.player1Id) shouldBe 3
                }
            }
        }

        context("Max speed (CR 702.178a / 702.179e)") {

            test("caps at 4 and never goes past it") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                }

                // Drive the speed up directly: the increase path is what the inherent trigger uses,
                // and the clamp lives in SpeedService rather than the trigger's intervening-if alone.
                var state = game.state
                repeat(10) {
                    state = SpeedService.change(state, game.player1Id, 1, sourceName = "test").first
                }

                withClue("CR 702.179e — max speed is exactly 4, so the value stops there") {
                    state.speed(game.player1Id) shouldBe Speed.MAX
                }
            }

            test("increasing from no speed lands on the amount (CR 702.179c)") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Plain Bear", summoningSickness = false)
                }
                game.state.hasSpeed(game.player1Id) shouldBe false

                val (state, events) = SpeedService.change(game.state, game.player1Id, 3, sourceName = "test")

                withClue("A player with no speed told to increase by 3 ends up at 3, not 4") {
                    state.speed(game.player1Id) shouldBe 3
                }
                withClue("The change is announced so triggers and the client can react") {
                    events.size shouldBe 1
                }
            }

            test("gates a static ability: double strike only at speed 4") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Max Speed Keyword", summoningSickness = false)
                }
                val goblin = game.findPermanent("Test Max Speed Keyword")!!

                withClue("At speed 1 the max-speed ability isn't there") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe false
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("At speed 4 the object has the granted ability (CR 702.178a)") {
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe true
                }
            }

            test("gates an activated ability: not a legal activation below speed 4") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Max Speed Activated", summoningSickness = false)
                }

                val artifact = game.findPermanent("Test Max Speed Activated")!!
                val abilityId = cardRegistry.getCard("Test Max Speed Activated")!!
                    .script.activatedAbilities[0].id
                val lifeBefore = game.getLifeTotal(1)

                val blocked = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = artifact, abilityId = abilityId)
                )
                withClue("Below max speed the ability doesn't exist, so activating it fails") {
                    (blocked.error != null).shouldBeTrue()
                    game.getLifeTotal(1) shouldBe lifeBefore
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val allowed = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = artifact, abilityId = abilityId)
                )
                withClue("At max speed it activates: ${allowed.error}") { allowed.error shouldBe null }
                game.resolveStack()
                withClue("…and resolves for 2 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 2
                }
            }

            test("gates a triggered ability: no attack trigger below speed 4") {
                val belowMax = speedGame {
                    withCardOnBattlefield(1, "Test Max Speed Triggered", summoningSickness = false)
                }
                val lifeBefore = belowMax.getLifeTotal(1)

                belowMax.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                belowMax.declareAttackers(mapOf("Test Max Speed Triggered" to 2))
                belowMax.resolveStack()

                withClue("Below max speed the creature has no attack trigger") {
                    belowMax.getLifeTotal(1) shouldBe lifeBefore
                }

                val atMax = speedGame {
                    withCardOnBattlefield(1, "Test Max Speed Triggered", summoningSickness = false)
                }
                atMax.state = SpeedService.set(atMax.state, atMax.player1Id, Speed.MAX, "test").first
                val lifeAtMax = atMax.getLifeTotal(1)

                atMax.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                atMax.declareAttackers(mapOf("Test Max Speed Triggered" to 2))
                atMax.resolveStack()

                withClue("At max speed the attack trigger fires for 3 life") {
                    atMax.getLifeTotal(1) shouldBe lifeAtMax + 3
                }
            }

            test("the max-speed condition is exact: speed 3 does not qualify") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Max Speed Keyword", summoningSickness = false)
                }
                val goblin = game.findPermanent("Test Max Speed Keyword")!!

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX - 1, "test").first
                withClue("3 is not max speed") {
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe false
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first
                withClue("4 is") {
                    game.state.projectedState.hasKeyword(goblin, Keyword.DOUBLE_STRIKE) shouldBe true
                }
            }

            test("a reduction lowers speed and can switch a max-speed ability back off") {
                val game = speedGame {
                    withCardOnBattlefield(2, "Test Max Speed Keyword", summoningSickness = false)
                    withCardInHand(1, "Test Slow Down")
                    withLandsOnBattlefield(1, "Island", 1)
                }
                val opponentGoblin = game.findPermanent("Test Max Speed Keyword")!!
                game.state = SpeedService.set(game.state, game.player2Id, Speed.MAX, "test").first
                withClue("The opponent is at max speed, so their ability is on") {
                    game.state.projectedState.hasKeyword(opponentGoblin, Keyword.DOUBLE_STRIKE) shouldBe true
                }

                val cast = game.castSpell(1, "Test Slow Down")
                withClue("Casting the speed reduction should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Speed really came down — it is not monotonic (Spikeshell Harrier)") {
                    game.state.speed(game.player2Id) shouldBe Speed.MAX - 1
                }
                withClue("…and the max-speed ability switched off with it") {
                    game.state.projectedState.hasKeyword(opponentGoblin, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }

            test("a reduction respects the card's floor and never grants speed to a player who has none") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Plain Bear", summoningSickness = false)
                }

                withClue("At the floor already: reducing by 1 with a floor of 1 does nothing") {
                    val atFloor = SpeedService.set(game.state, game.player2Id, Speed.STARTING, "test").first
                    val (reduced, events) = SpeedService.change(
                        atFloor, game.player2Id, -1, minimum = Speed.STARTING, sourceName = "test"
                    )
                    reduced.speed(game.player2Id) shouldBe Speed.STARTING
                    events shouldBe emptyList()
                }

                withClue("A player with no speed is never handed the designation by a reduction") {
                    game.state.hasSpeed(game.player2Id) shouldBe false
                    val (reduced, events) = SpeedService.change(
                        game.state, game.player2Id, -1, minimum = Speed.STARTING, sourceName = "test"
                    )
                    reduced.hasSpeed(game.player2Id) shouldBe false
                    reduced.speed(game.player2Id) shouldBe Speed.NONE
                    events shouldBe emptyList()
                }
            }

            test("one player's max speed does not switch on an opponent's max-speed ability") {
                val game = speedGame {
                    withCardOnBattlefield(2, "Test Max Speed Keyword", summoningSickness = false)
                }
                val opponentGoblin = game.findPermanent("Test Max Speed Keyword")!!

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("\"your speed\" is the ability controller's speed, not any player's") {
                    game.state.projectedState.hasKeyword(opponentGoblin, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }
        }

        context("Reading a player's speed (CR 702.179f)") {

            test("reads as 0 for a player with no speed") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Speed Scaler", summoningSickness = false)
                }
                val scaler = game.findPermanent("Test Speed Scaler")!!

                withClue("No speed means the +X/+0 adds nothing — not an error, and not null") {
                    game.state.hasSpeed(game.player1Id) shouldBe false
                    game.state.projectedState.getPower(scaler) shouldBe 1
                }
            }

            test("feeds a static +X/+0 in projection and tracks every speed step") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Speed Scaler", summoningSickness = false)
                }
                val scaler = game.findPermanent("Test Speed Scaler")!!

                for (speed in 1..Speed.MAX) {
                    game.state = SpeedService.set(game.state, game.player1Id, speed, "test").first
                    withClue("At speed $speed the scaler is a ${1 + speed}/1") {
                        game.state.projectedState.getPower(scaler) shouldBe 1 + speed
                    }
                }
            }

            test("feeds a resolving spell's amount") {
                val game = speedGame {
                    withCardOnBattlefield(1, "Test Engine Starter", summoningSickness = false)
                    withCardInHand(1, "Test Speed Siphon")
                    withLandsOnBattlefield(1, "Swamp", 1)
                }
                game.state = SpeedService.set(game.state, game.player1Id, 3, "test").first
                val lifeBefore = game.getLifeTotal(1)

                game.castSpell(1, "Test Speed Siphon")
                game.resolveStack()

                withClue("\"You gain life equal to your speed\" gains 3 at speed 3") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 3
                }
            }
        }

        context("Descriptions and keyword surface") {

            test("startYourEngines() adds only the keyword — the SBA does the rest") {
                val def = cardRegistry.getCard("Test Engine Starter")!!
                def.keywords shouldBe setOf(Keyword.START_YOUR_ENGINES)
                withClue("No trigger or static is needed on the card") {
                    def.script.triggeredAbilities shouldBe emptyList()
                    def.script.staticAbilities shouldBe emptyList()
                }
            }

            test("maxSpeed { } tags the keyword and gates each ability it declares") {
                val def = cardRegistry.getCard("Test Max Speed Activated")!!
                withClue("The keyword drives the card's badge") {
                    def.keywords.contains(Keyword.MAX_SPEED) shouldBe true
                }
                val ability = def.script.activatedAbilities.single()
                withClue("The gate is an activation restriction on the max-speed condition") {
                    ability.restrictions.contains(
                        com.wingedsheep.sdk.scripting.ActivationRestriction
                            .OnlyIfCondition(Conditions.YouHaveMaxSpeed)
                    ) shouldBe true
                }
                withClue("The ability's label carries the printed prefix: ${ability.description}") {
                    ability.description.startsWith("Max speed — ") shouldBe true
                }
            }
        }
    }

    /**
     * Two-player board in [activePlayer]'s precombat main phase — the shared setup for every test
     * here. [extra] adds the permanents/cards that test needs.
     *
     * Deliberately built one step *earlier* (upkeep) and then advanced: the CR 704.5z state-based
     * action fires when the engine polls state-based actions, and the poll this crosses is the one
     * after the draw step (`TurnManager`) — so arriving in the main phase through a real step
     * sequence is what proves the check is wired into the game loop rather than merely callable. A
     * plain `passPriority()` would neither poll SBAs nor leave the active player holding priority.
     *
     * Both libraries are stocked so crossing the draw step (here and when a test rounds the table)
     * can't deck anyone and turn every assertion into a loss check.
     */
    private fun speedGame(activePlayer: Int = 1, extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(12) {
            builder.withCardInLibrary(1, "Test Plain Bear")
            builder.withCardInLibrary(2, "Test Plain Bear")
        }
        builder.extra()
        val game = builder
            .withActivePlayer(activePlayer)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /** Advance from the active player's main phase all the way round to player 1's next main phase. */
    private fun TestGame.roundTheTableToPlayerOne() {
        do {
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        } while (state.activePlayerId != player1Id)
    }
}
