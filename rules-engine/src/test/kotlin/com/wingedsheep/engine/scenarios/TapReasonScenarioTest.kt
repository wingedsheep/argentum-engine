package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.TriggerBinding
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

/**
 * The **tap-reason** primitive: [TapReason] on [TappedEvent], matched by
 * [EventPattern.TapEvent.reason].
 *
 * Named for the mechanic rather than for a card because it is engine vocabulary — Agent Maria Hill
 * (MSH #2) is its first reader, and her own behaviour lives in `AgentMariaHillScenarioTest`. What
 * these tests pin is the *axis*: which tap sites name a cause, which deliberately don't, and that
 * asking for no cause still matches every tap.
 *
 * The cause has to be applied at all **three** match sites, so all three are covered here: the
 * per-event one (`TriggerMatcher`, exercised by every SELF case below), the ATTACHED one
 * (`AttachmentTriggerDetector`, via `Teamwork Tether`) and the batch one
 * (`TriggerDetector.detectTapBatchTriggers`, via `Teamwork Roll Call`). The latter two have no
 * printed card, so they use test-only cards — without them a regression at either site is invisible.
 *
 * The claim that matters is the negative one. Teamwork, crew, attacking and a mana payment are all
 * taps performed by the permanent's own controller, so `TappedEvent.tappedById` is identical across
 * them; if the reason were not carried, or were guessed at, a "becomes tapped to pay a teamwork
 * cost" trigger would fire on all four. Only teamwork is classified today (CR 702.194a), and each
 * unclassified site is asserted to report [TapReason.UNSPECIFIED] rather than anything else.
 */
class TapReasonScenarioTest : ScenarioTestBase() {

    private fun List<GameEvent>.tapsOf(entityId: EntityId): List<TappedEvent> =
        filterIsInstance<TappedEvent>().filter { it.entityId == entityId }

    /**
     * The ATTACHED reader: "Whenever enchanted creature becomes tapped to pay a teamwork cost, draw
     * a card." No printed card wants this shape, but `becomesTapped(binding = ATTACHED, reason = …)`
     * is authorable, so the aura path has to narrow by cause like the other two match sites.
     */
    private val teamworkTether = card("Teamwork Tether") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\n" +
            "Whenever enchanted creature becomes tapped to pay a teamwork cost, draw a card."
        auraTarget = Targets.Creature
        triggeredAbility {
            trigger = Triggers.becomesTapped(
                binding = TriggerBinding.ATTACHED,
                reason = TapReason.TEAMWORK,
            )
            effect = Effects.DrawCards(1)
        }
    }

    /**
     * The batch reader: "Whenever one or more creatures you control become tapped to pay a teamwork
     * cost, draw a card." Fires once per batch (CR 603.2c), not once per creature.
     */
    private val teamworkRollCall = card("Teamwork Roll Call") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        oracleText = "Whenever one or more creatures you control become tapped to pay a teamwork " +
            "cost, draw a card."
        triggeredAbility {
            trigger = Triggers.OneOrMoreBecomeTapped(
                GameObjectFilter.Creature.youControl(),
                reason = TapReason.TEAMWORK,
            )
            effect = Effects.DrawCards(1)
        }
    }

    init {
        cardRegistry.register(teamworkTether)
        cardRegistry.register(teamworkRollCall)

        context("tap reason") {

            test("a creature tapped to pay a teamwork cost carries TapReason.TEAMWORK") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                val result = game.castSpellWithTeamwork(1, "Repulsor Blast", "Craw Wurm", targetId = wall)
                result.error shouldBe null

                val tap = result.events.tapsOf(wurm).single()
                tap.reason shouldBe TapReason.TEAMWORK
                withClue("attribution is a separate axis and stays the controller's") {
                    tap.tappedById shouldBe game.player1Id
                }

                withClue("the mana payment for the same cast taps lands, and a land tap names no cause") {
                    val landTaps = result.events.filterIsInstance<TappedEvent>()
                        .filter { it.entityName == "Mountain" }
                    landTaps.shouldNotBeEmpty()
                    landTaps.map { it.reason }.toSet() shouldBe setOf(TapReason.UNSPECIFIED)
                }
            }

            test("the same spell cast without teamwork taps nothing and names no cause") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                val result = game.castSpell(1, "Repulsor Blast", targetId = wall)
                result.error shouldBe null

                result.events.tapsOf(wurm).shouldBeEmpty()
                game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                withClue("no teamwork was declared, so no tap anywhere in the cast may claim it") {
                    result.events.filterIsInstance<TappedEvent>()
                        .none { it.reason == TapReason.TEAMWORK } shouldBe true
                }
            }

            test("declaring an attacker names no cause") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Craw Wurm" to 2))
                result.error shouldBe null

                val tap = result.events.tapsOf(wurm).single()
                withClue("the attacker tap is performed by the same player as a teamwork tap, so " +
                    "only the cause separates them — and this cause is not classified") {
                    tap.reason shouldBe TapReason.UNSPECIFIED
                    tap.tappedById shouldBe game.player1Id
                }
            }

            test("crewing a Vehicle names no cause") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Careening Mine Cart")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cart = game.findPermanent("Careening Mine Cart").shouldNotBeNull()

                val result = game.execute(CrewVehicle(game.player1Id, cart, listOf(wurm)))
                result.error shouldBe null

                withClue("crew is the same 'tap creatures with total power N or more' selection as " +
                    "teamwork, on the ability rail — the nearest miss there is") {
                    result.events.tapsOf(wurm).single().reason shouldBe TapReason.UNSPECIFIED
                }
            }

            test("a cause-agnostic 'becomes tapped' trigger still fires on a teamwork tap") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // "Whenever this creature becomes tapped during your turn, untap it" — no cause
                    // named, so it must keep matching every tap, teamwork included. At power 0 it
                    // contributes nothing to the threshold; the Wurm carries it.
                    .withCardOnBattlefield(1, "Interface Ace")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ace = game.findPermanent("Interface Ace").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                val result = game.castSpellWithTeamwork(
                    1, "Repulsor Blast", "Interface Ace", "Craw Wurm", targetId = wall,
                )
                result.error shouldBe null
                result.events.tapsOf(ace).single().reason shouldBe TapReason.TEAMWORK
                game.state.getEntity(ace)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                withClue("its untap trigger saw the teamwork tap, because a null reason on the " +
                    "pattern asks for no particular cause") {
                    game.state.getEntity(ace)?.has<TappedComponent>() shouldBe false
                }
            }

            // The ATTACHED binding is its own match site (`AttachmentTriggerDetector`), separate
            // from the per-event one in `TriggerMatcher`, so the cause has to be applied there too.
            test("an ATTACHED trigger narrows by cause on the enchanted creature's tap") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Teamwork Tether")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                game.castSpell(1, "Teamwork Tether", targetId = wurm).error shouldBe null
                game.resolveStack()

                game.castSpellWithTeamwork(1, "Repulsor Blast", "Craw Wurm", targetId = wall)
                    .error shouldBe null
                game.resolveStack()

                withClue("the enchanted creature's teamwork tap fires the aura's ATTACHED trigger") {
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Island") shouldBe true
                }
            }

            test("an ATTACHED trigger stays silent when the enchanted creature is tapped to attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Teamwork Tether")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                game.castSpell(1, "Teamwork Tether", targetId = wurm).error shouldBe null
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 2)).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 1
            }

            // The batch site (`TriggerDetector.detectTapBatchTriggers`) narrows by cause the same
            // way it narrows by tapper: the batch is filtered down to the matching taps, then fires
            // once (CR 603.2c) — not once per tapped permanent, and not discarded because the batch
            // also held taps from an unnamed cause.
            test("a batch trigger fires once for a multi-creature teamwork payment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Teamwork Roll Call")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                game.castSpellWithTeamwork(
                    1, "Repulsor Blast", "Craw Wurm", "Grizzly Bears", targetId = wall,
                ).error shouldBe null
                game.resolveStack()

                withClue("two creatures tapped in one payment is one batch, so one card drawn") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("a batch trigger stays silent when the same creatures are tapped to attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Teamwork Roll Call")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("declaring attackers taps both at once — a batch with no named cause") {
                    game.handSize(1) shouldBe 0
                    game.librarySize(1) shouldBe 1
                }
            }

            test("the pattern renders the cause in its description and defaults to cause-agnostic") {
                EventPattern.TapEvent().reason shouldBe null
                EventPattern.TapEvent().description shouldBe "a permanent becomes tapped"
                EventPattern.TapEvent(reason = TapReason.TEAMWORK).description shouldContain
                    "to pay a teamwork cost"
            }

            test("the reason round-trips through serialization and defaults when absent") {
                val json = Json { serializersModule = engineSerializersModule }

                val teamworkTap: GameEvent = TappedEvent(
                    entityId = EntityId.of("e1"),
                    entityName = "Agent Maria Hill",
                    tappedById = EntityId.of("player-1"),
                    reason = TapReason.TEAMWORK,
                )
                val decoded = json.decodeFromString<GameEvent>(json.encodeToString(teamworkTap))
                decoded shouldBe teamworkTap

                withClue("an event encoded before the field existed must still decode") {
                    val legacy = """{"type":"TappedEvent","entityId":"e1","entityName":"Agent Maria Hill"}"""
                    val legacyTap = json.decodeFromString<GameEvent>(legacy) as TappedEvent
                    legacyTap.reason shouldBe TapReason.UNSPECIFIED
                }
            }
        }
    }
}
