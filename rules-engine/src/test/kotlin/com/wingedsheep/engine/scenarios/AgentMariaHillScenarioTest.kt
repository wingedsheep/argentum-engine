package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Agent Maria Hill (MSH #2) — {W} Legendary Creature — Human Spy Hero, 2/1.
 *
 *   Whenever Agent Maria Hill becomes tapped to pay a teamwork cost, put a +1/+1 counter on her
 *   and draw a card.
 *
 * The whole card is the *discrimination*, so every test here is about which taps count. Repulsor
 * Blast ({3}{R} sorcery, Teamwork 2) is the vehicle for the teamwork half: at power 2 she clears
 * its threshold on her own, so a cast that taps her alone is a legal payment.
 *
 * The negative cases are the ones that would silently pass on a naive
 * "whenever this becomes tapped" implementation: attacking, crewing, an opponent's tap effect, and
 * a teamwork cast paid by somebody else all tap a creature. The first two are performed by her own
 * controller — so nothing but the tap's recorded *cause*
 * ([com.wingedsheep.sdk.scripting.TapReason]) can separate them from a teamwork tap — and the third
 * is the only one where `tappedById` differs, which is where cause and attribution could be
 * confused for one another.
 *
 * [com.wingedsheep.sdk.scripting.TapReason] itself names only two members — `UNSPECIFIED` and
 * `TEAMWORK`. Two of the unclassified causes listed on `UNSPECIFIED` (a mana ability and a `{T}`
 * activation cost) cannot be aimed at *her* anyway: she has neither. Those directions, plus the
 * event/predicate mechanics themselves, are pinned on the shared axis in `TapReasonScenarioTest`.
 */
class AgentMariaHillScenarioTest : ScenarioTestBase() {

    /** +1/+1 counters currently on the named permanent. */
    private fun TestGame.plusOneCounters(name: String): Int {
        val id = findPermanent(name) ?: error("'$name' is not on the battlefield")
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Agent Maria Hill") {

            test("tapped to pay a teamwork cost, she gets a +1/+1 counter and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                // Teamwork 2 — her printed power 2 clears the threshold on its own.
                game.castSpellWithTeamwork(1, "Repulsor Blast", "Agent Maria Hill", targetId = wall)
                    .error shouldBe null
                game.state.getEntity(hill)?.has<TappedComponent>() shouldBe true

                // The cost is paid during casting, so the trigger only reaches the stack once the
                // spell has become cast (CR 601.2i) — and CR 603.3 puts it on top, so it resolves
                // before the spell it paid for.
                withClue("her trigger sits on top of the spell that it paid for") {
                    val top = game.state.getTopOfStack().shouldNotBeNull()
                    game.state.getEntity(top)?.get<TriggeredAbilityOnStackComponent>()
                        .shouldNotBeNull()
                        .sourceName shouldBe "Agent Maria Hill"
                    game.state.stack.size shouldBe 2
                }

                game.resolveStack()

                withClue("the trigger's own half: a +1/+1 counter on her") {
                    game.plusOneCounters("Agent Maria Hill") shouldBe 1
                    game.state.projectedState.getPower(hill) shouldBe 3
                    game.state.projectedState.getToughness(hill) shouldBe 2
                }
                withClue("and a card drawn — the library's only card is now in hand") {
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Plains") shouldBe true
                    game.librarySize(1) shouldBe 0
                }
            }

            test("tapped by attacking, she does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Agent Maria Hill" to 2)).error shouldBe null

                withClue("declaring an attacker taps it (CR 508.1f) — her own controller's tap, " +
                    "exactly like a teamwork payment, so only the cause tells them apart") {
                    game.state.getEntity(hill)?.has<TappedComponent>() shouldBe true
                }
                game.resolveStack()
                game.plusOneCounters("Agent Maria Hill") shouldBe 0
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 1
            }

            test("tapped to crew a Vehicle, she does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardOnBattlefield(1, "Careening Mine Cart")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()
                val cart = game.findPermanent("Careening Mine Cart").shouldNotBeNull()

                // Crew 1: the same "tap creatures with total power N or more" selection teamwork
                // makes, on the ability rail instead of the cast rail.
                game.execute(CrewVehicle(game.player1Id, cart, listOf(hill))).error shouldBe null
                game.state.getEntity(hill)?.has<TappedComponent>() shouldBe true

                game.resolveStack()
                game.plusOneCounters("Agent Maria Hill") shouldBe 0
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 1
            }

            // The one direction where `tappedById` is *not* her controller, so it is the one place
            // the cause and the attribution could plausibly be confused for each other.
            test("tapped by an opponent's effect, she does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(2, "Crippling Chill")
                    .withCardInLibrary(2, "Island")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()

                // Crippling Chill: "Tap target creature. …" — a real untapped -> tapped transition,
                // caused by the opponent rather than by her controller.
                game.passPriority() // active player passes, so the opponent can cast at instant speed
                game.castSpell(2, "Crippling Chill", targetId = hill).error shouldBe null
                game.resolveStack()

                game.state.getEntity(hill)?.has<TappedComponent>() shouldBe true
                withClue("an opponent's tap effect names no cause, so the teamwork trigger stays silent") {
                    game.plusOneCounters("Agent Maria Hill") shouldBe 0
                    game.handSize(1) shouldBe 0
                    game.librarySize(1) shouldBe 1
                }
            }

            test("a teamwork cost paid by another creature does not trigger her") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()

                // The 6/4 Craw Wurm pays the whole teamwork 2 cost; she is never tapped.
                game.castSpellWithTeamwork(1, "Repulsor Blast", "Craw Wurm", targetId = wall)
                    .error shouldBe null
                game.resolveStack()

                withClue("the trigger is on *her* becoming tapped, not on a teamwork spell being cast") {
                    game.state.getEntity(hill)?.has<TappedComponent>() shouldBe false
                    game.plusOneCounters("Agent Maria Hill") shouldBe 0
                    game.handSize(1) shouldBe 0
                }
            }

            // The tests above drive `execute(...)` and so prove only what the handler accepts. This
            // one goes against `getLegalActions` — what the client actually sees — because three
            // enumeration bugs in this mechanic have all been in the advertising path, invisible to
            // tests that cast directly.
            test("the enumerator still offers the teamwork cast and prices her as an eligible payer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Agent Maria Hill")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hill = game.findPermanent("Agent Maria Hill").shouldNotBeNull()

                val teamworkCast = game.getLegalActions(1)
                    .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                    .shouldNotBeNull()

                teamworkCast.description shouldBe "Cast Repulsor Blast (Teamwork 2)"
                val info = teamworkCast.additionalCostInfo.shouldNotBeNull()
                info.tapForPowerRequired shouldBe 2
                withClue("she is untapped and controlled by the caster, so she is offered as a " +
                    "payer, at her projected power") {
                    val candidate = info.tapForPowerCreatures.firstOrNull { it.entityId == hill }
                        .shouldNotBeNull()
                    candidate.power shouldBe 2
                }
            }
        }
    }
}
