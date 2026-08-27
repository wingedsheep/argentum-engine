package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Two-Headed Giant — Phase 6: the server session/DTO/masking layer must surface the engine's team
 * model (Phases 1–5) without disturbing the 2-player / Free-for-All paths. These tests drive a real
 * 4-seat [GameSession] under [Format.TwoHeadedGiant] (no WebSocket plumbing) and assert the
 * team-aware seat roster, opponent list, shared life resolution, and teammate hand visibility — and
 * that a non-team game is the unchanged degenerate case of the same code path.
 *
 * Teams are seats 0+1 (team 0) vs 2+3 (team 1) in join/turn order.
 */
class TwoHeadedGiantSessionTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** A started 4-seat Two-Headed Giant session (40-Forest decks), with player ids player-1..4. */
    private fun started2hg(): Pair<GameSession, List<EntityId>> {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 4)
        session.engineFormat = Format.TwoHeadedGiant()
        session.teams = listOf(listOf(0, 1), listOf(2, 3))
        val ids = (1..4).map { EntityId.of("player-$it") }
        ids.forEachIndexed { i, id ->
            session.addPlayer(PlayerSession(mockWs("ws$i"), id, "Player${i + 1}"), mapOf("Forest" to 40))
        }
        session.startGame()
        return session to ids
    }

    init {
        test("seat roster carries each seat's team index, with teammates in adjacent seats (CR 805.1)") {
            val (session, ids) = started2hg()

            val roster = session.seatInfos(viewerId = ids[0]).sortedBy { it.seatIndex }
            roster.forEach { it.teamIndex shouldNotBe null }
            roster.single { it.isYou }.playerId shouldBe ids[0].value

            // The two members of each team occupy consecutive seats — the seating precondition
            // for shared team turns (CR 805.1). Which team is seated first is randomized (805.3).
            roster.groupBy { it.teamIndex }.values.forEach { members ->
                members shouldHaveSize 2
                (members.maxOf { it.seatIndex } - members.minOf { it.seatIndex }) shouldBe 1
            }

            // Team membership follows the config partition (seats 0+1 vs 2+3), independent of seating.
            val teamOf = roster.associate { it.playerId to it.teamIndex }
            teamOf[ids[0].value] shouldBe teamOf[ids[1].value]
            teamOf[ids[2].value] shouldBe teamOf[ids[3].value]
            (teamOf[ids[0].value] == teamOf[ids[2].value]) shouldBe false
        }

        test("both heads of the surviving team are winners, not just the representative (CR 810.8a)") {
            val (session, ids) = started2hg()
            // Team 1 (ids[2], ids[3]) concedes; one concession takes the whole team out (CR 810.8b).
            session.playerConcedes(ids[2])
            session.isGameOver() shouldBe true
            session.getWinnerIds() shouldBe listOf(ids[0], ids[1])
            session.getWinnerId() shouldBe ids[0]
        }

        test("a teammate is not an opponent (CR 810): getOpponentIds returns only the other team") {
            val (session, ids) = started2hg()

            // p0's opponents are the other team (p2, p3) — never the teammate p1.
            session.getOpponentIds(ids[0]) shouldContainExactlyInAnyOrder listOf(ids[2], ids[3])
            session.getOpponentIds(ids[2]) shouldContainExactlyInAnyOrder listOf(ids[0], ids[1])
        }

        test("life totals are the shared team total for both teammates, not each raw component") {
            val (session, ids) = started2hg()

            // Drive team 0's shared total to 25 (resolver writes the canonical owner = first member),
            // then corrupt the teammate's own raw component to a different value. getLifeTotals must
            // report the team total (25) for BOTH teammates — proving it routes through the resolver,
            // not the raw LifeTotalComponent. Team 1 is untouched (30 from the 2HG starting life).
            var s = session.getStateForTesting()!!
            s = s.withLifeTotal(ids[0], 25)
            s = s.updateEntity(ids[1]) { it.with(LifeTotalComponent(99)) }
            session.injectStateForDevScenario(s)
            ids.forEachIndexed { i, id ->
                session.addPlayer(PlayerSession(mockWs("ws$i"), id, "Player${i + 1}"), mapOf("Forest" to 40))
            }

            session.getLifeTotals() shouldContainExactly listOf(25, 25, 30, 30)
        }

        test("a player sees their teammate's hand but not an opponent's (CR 810.2)") {
            val (session, ids) = started2hg()
            val viewer = ids[0]
            val teammate = ids[1]

            val update = session.createStateUpdate(viewer, emptyList()) as ServerMessage.StateUpdate
            val handZones = update.state.zones.filter { it.zoneId.zoneType == Zone.HAND }

            val ownHand = handZones.single { it.zoneId.ownerId == viewer }
            val teammateHand = handZones.single { it.zoneId.ownerId == teammate }
            val opponentHands = handZones.filter { it.zoneId.ownerId in listOf(ids[2], ids[3]) }

            // Own hand and the teammate's hand are both fully visible with their cards populated.
            ownHand.isVisible shouldBe true
            teammateHand.isVisible shouldBe true
            teammateHand.cardIds.size shouldBe teammateHand.size
            teammateHand.cardIds.size shouldBe ownHand.cardIds.size

            // Opponents' hands keep their count but hide their contents.
            opponentHands.forEach {
                it.isVisible shouldBe false
                it.cardIds.size shouldBe 0
            }
        }

        test("the seat roster carries the shared-turn flag, which the client needs to answer \"may I act?\"") {
            val (session, ids) = started2hg()

            // Two axes, not one: 2HG shares life *and* turns, Team vs. Team shares neither, and the
            // client can't derive "a teammate may act in my priority window" (CR 805.5a) from
            // either "has a team" or "pools life".
            session.seatInfos(viewerId = ids[0]).forEach {
                it.teamSharedLife shouldBe true
                it.teamSharedTurns shouldBe true
            }
        }

        test("a teammate is offered their OWN actions while their partner holds the baton (CR 805.5)") {
            val (session, ids) = started2hg()
            val state = session.getStateForTesting()!!
            val baton = state.priorityPlayerId!!
            val teammate = state.teamOf(baton).single { it != baton }
            val opponent = state.getOpponents(baton).first()

            // The baton holder gets actions, as always.
            session.getLegalActions(baton) shouldNotBe emptyList<Any>()

            // So does their teammate — this is the whole of team priority from the server's side.
            // The actions must be enumerated for the *teammate's* seat: it is their hand and their
            // mana, and an action tagged with the baton holder's id would be rejected as theirs.
            val teammateActions = session.getLegalActions(teammate)
            teammateActions shouldNotBe emptyList<Any>()
            teammateActions.forEach { it.action.playerId shouldBe teammate }

            // The opposing team is still shut out — team priority widens the team, not the table.
            session.getLegalActions(opponent) shouldBe emptyList()
        }

        test("a Free-for-All pod stays one seat at a time — only the baton holder is offered actions") {
            val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 4)
            val ids = (1..4).map { EntityId.of("ffa-$it") }
            ids.forEachIndexed { i, id ->
                session.addPlayer(PlayerSession(mockWs("f$i"), id, "P${i + 1}"), mapOf("Forest" to 40))
            }
            session.startGame()

            val baton = session.getStateForTesting()!!.priorityPlayerId!!
            session.getLegalActions(baton) shouldNotBe emptyList<Any>()
            ids.filter { it != baton }.forEach { session.getLegalActions(it) shouldBe emptyList() }
        }

        test("a revealed card in an opponent's hand still reaches the viewer, count and all") {
            // The client collapses a *fully* face-down opponent hand to a count and falls back to a
            // real fan the moment any of it is revealed. That decision reads `zone.cardIds`, so this
            // pins the contract it reads: a hidden hand ships the revealed cards' ids (and their
            // details) while `size` stays the true hand size. Revealed cards in hand are routine —
            // anything returned to hand from the battlefield, a graveyard or the stack stays known
            // to the table until its owner plays a same-named card (RevealedInHandTracker).
            val (session, ids) = started2hg()
            val viewer = ids[0]
            val opponent = ids[2]

            var s = session.getStateForTesting()!!
            val opponentHand = s.getZone(com.wingedsheep.engine.state.ZoneKey(opponent, Zone.HAND))
            val known = opponentHand.first()
            s = s.updateEntity(known) {
                it.with(com.wingedsheep.engine.state.components.identity.RevealedToComponent.to(viewer))
            }
            session.injectStateForDevScenario(s)

            val update = session.createStateUpdate(viewer, emptyList()) as ServerMessage.StateUpdate
            val hand = update.state.zones.single {
                it.zoneId.zoneType == Zone.HAND && it.zoneId.ownerId == opponent
            }

            // Exactly the revealed card comes through, and its details are populated — that is what
            // the fan draws face-up. The hand's real size is unchanged, so the count on the name
            // plate and the face-down placeholders beside it both stay honest.
            hand.cardIds shouldContainExactly listOf(known)
            update.state.cards[known] shouldNotBe null
            hand.size shouldBe opponentHand.size
            hand.size shouldBe 7
            // The other seat on that team revealed nothing, so it still collapses to a count.
            update.state.zones.single {
                it.zoneId.zoneType == Zone.HAND && it.zoneId.ownerId == ids[3]
            }.cardIds.shouldBeEmpty()
        }

        test("non-team game is unchanged: no team index, every other seat is an opponent") {
            // No teams / Standard format — the degenerate case of the same code path.
            val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
            val ids = (1..2).map { EntityId.of("solo-$it") }
            ids.forEachIndexed { i, id ->
                session.addPlayer(PlayerSession(mockWs("s$i"), id, "P${i + 1}"), mapOf("Forest" to 40))
            }
            session.startGame()

            session.seatInfos(viewerId = ids[0]).forEach {
                it.teamIndex shouldBe null
                it.teamSharedTurns shouldBe false
            }
            session.getOpponentIds(ids[0]) shouldContainExactly listOf(ids[1])
        }
    }
}
