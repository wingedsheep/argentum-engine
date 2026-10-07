package com.wingedsheep.gameserver.jumpstart

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.lobby.*
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.GameRules
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

class JumpstartLobbyTest : FunSpec({
    val sets = MtgSetCatalog.all
    val registry = CardRegistry().apply { sets.forEach { register(it.cards + it.basicLands) } }
    val generator = BoosterGenerator(sets.associate { set ->
        set.code to BoosterGenerator.SetConfig(set.code, set.displayName, set.cards, set.basicLands)
    })
    val host = EntityId("host")
    val guest = EntityId("guest")
    fun lobby(format: TournamentFormat = TournamentFormat.SEALED) = TournamentLobby(
        setCodes = listOf("JMP"), setNames = listOf("Jumpstart"), boosterGenerator = generator, format = format,
    ).apply {
        addPlayer(PlayerIdentity(playerId = host, playerName = "Host"))
        addPlayer(PlayerIdentity(playerId = guest, playerName = "Guest"))
    }

    test("all published lists contain twenty cards and the three starter themes are playable") {
        JumpstartPacks.lists.size shouldBe 121
        JumpstartPacks.lists.values.all { it.size == 20 } shouldBe true
        JumpstartPacks(generator).packs.map { it.id }.shouldContainAll("Archaeology (4)", "Unicorns", "Goblins (4)")
    }

    test("Jumpstart defaults on for JMP alone in draft and sealed but never mixed sets or commander") {
        for (format in listOf(TournamentFormat.SEALED, TournamentFormat.DRAFT)) {
            val l = lobby(format)
            l.isJumpstart shouldBe true
            l.setCodes = listOf("JMP", "M21")
            l.isJumpstart shouldBe false
            l.setCodes = listOf("JMP")
            l.useJumpstart = false
            l.isJumpstart shouldBe false
            l.useJumpstart = true
            l.rules = GameRules.COMMANDER
            l.isJumpstart shouldBe false
        }
        lobby(TournamentFormat.WINSTON_DRAFT).isJumpstart shouldBe false
    }

    test("two authorized picks make an exact forty-card deck without adding basic lands") {
        val l = lobby()
        l.startJumpstart(guest) shouldBe false
        l.startJumpstart(host) shouldBe true
        l.startJumpstart(host) shouldBe false
        for (id in listOf(host, guest)) {
            l.players.getValue(id).jumpstartOffers.size shouldBe 3
            l.pickJumpstart(id, "unoffered", 1) shouldBe false
            val first = l.players.getValue(id).jumpstartOffers.first()
            l.pickJumpstart(id, first, 1) shouldBe true
            l.players.getValue(id).cardPool.size shouldBe 20
            l.pickJumpstart(id, first, 1) shouldBe false
            val second = l.players.getValue(id).jumpstartOffers.last()
            l.pickJumpstart(id, second, 2) shouldBe true
            val pool = l.players.getValue(id).cardPool
            pool.size shouldBe 40
            val expected = (JumpstartPacks.lists.getValue(first) + JumpstartPacks.lists.getValue(second)).groupingBy { it }.eachCount()
            pool.groupingBy { it.name }.eachCount() shouldBe expected
            l.submitDeck(id, expected + ("Plains" to 50)).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Error>()
            l.submitDeck(id, expected).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Success>()
            l.getSubmittedSideboard(id) shouldBe emptyMap()
            l.unsubmitDeck(id) shouldBe false
            l.pickJumpstart(id, second, 3) shouldBe false
        }
        l.allDecksSubmitted() shouldBe true
    }

    test("private offers and pick progress survive a server restart") {
        val l = lobby()
        l.startJumpstart(host) shouldBe true
        l.pickJumpstart(host, l.players.getValue(host).jumpstartOffers.first(), 1) shouldBe true
        val (restored, _) = restoreTournamentLobby(l.toPersistent(), registry, generator)
        restored.buildLobbyUpdate(host).jumpstart shouldBe l.buildLobbyUpdate(host).jumpstart
        restored.players.getValue(host).cardPool.map { it.name } shouldBe l.players.getValue(host).cardPool.map { it.name }
        restored.buildLobbyUpdate(guest).jumpstart!!.selectedPacks shouldBe emptyList()
        restored.pickJumpstart(host, restored.players.getValue(host).jumpstartOffers.first(), 2) shouldBe true
        restored.players.getValue(host).cardPool.size shouldBe 40
        l.useJumpstart = false
        restoreTournamentLobby(l.toPersistent(), registry, generator).first.useJumpstart shouldBe false
    }

    test("a missing or banned card disables a whole variant instead of replacing it") {
        val packs = JumpstartPacks(generator)
        packs.available(setOf("scuttlemutt")).none { it.id == "Archaeology (4)" } shouldBe true
        val missing = BoosterGenerator(generator.availableSets.mapValues { (_, set) ->
            set.copy(cards = set.cards.filterNot { it.name == "Scuttlemutt" })
        })
        JumpstartPacks(missing).packs.none { it.id == "Archaeology (4)" } shouldBe true
        val l = lobby().apply { bannedCardNames = setOf("Plains", "Island", "Mountain", "Forest", "Swamp") }
        l.jumpstartStartError().shouldNotBeNull()
        l.startJumpstart(host) shouldBe false
        l.state shouldBe LobbyState.WAITING_FOR_PLAYERS
    }
    test("J22 has 121 exact published variants spanning 46 themes") {
        JumpstartPacks.listsFor("J22").size shouldBe 121
        JumpstartPacks.listsFor("J22").values.all { it.size == 20 } shouldBe true
        JumpstartPacks.listsFor("J22").keys.map { it.substringBefore(" (") }.distinct().size shouldBe 46
        JumpstartPacks(generator, "J22").packs.map { it.theme }.distinct().size.let { it >= 3 } shouldBe true
    }

    test("switching from JMP to J22 uses J22 offers and restores an exact deck") {
        val l = lobby()
        // Materialize the JMP catalogue first, as the host may do before changing the set.
        l.jumpstartStartError() shouldBe null
        l.updateSets(listOf("J22")) shouldBe true
        l.isJumpstart shouldBe true
        l.startJumpstart(host) shouldBe true
        val lists = JumpstartPacks.listsFor("J22")
        l.players.getValue(host).jumpstartOffers.all { it in lists } shouldBe true
        val first = l.players.getValue(host).jumpstartOffers.first()
        l.pickJumpstart(host, first, 1) shouldBe true
        val (restored, _) = restoreTournamentLobby(l.toPersistent(), registry, generator)
        restored.buildLobbyUpdate(host).jumpstart shouldBe l.buildLobbyUpdate(host).jumpstart
        val second = restored.players.getValue(host).jumpstartOffers.first()
        restored.pickJumpstart(host, second, 2) shouldBe true
        val expected = (lists.getValue(first) + lists.getValue(second)).groupingBy { it }.eachCount()
        restored.players.getValue(host).cardPool.groupingBy { it.name }.eachCount() shouldBe expected
        restored.submitDeck(host, expected).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Success>()
        restored.getSubmittedSideboard(host) shouldBe emptyMap()
        restored.buildLobbyUpdate(guest).jumpstart!!.selectedPacks shouldBe emptyList()
    }

    test("J22 offers omit entire banned variants and mixed selections use traditional limited") {
        val packs = JumpstartPacks(generator, "J22")
        val pack = packs.packs.first()
        val banned = pack.cards.first().name
        packs.available(setOf(banned.lowercase())).none { it.id == pack.id } shouldBe true
        val missing = BoosterGenerator(generator.availableSets.mapValues { (_, set) ->
            set.copy(cards = set.cards.filterNot { it.name == banned }, basicLands = set.basicLands.filterNot { it.name == banned })
        })
        JumpstartPacks(missing, "J22").packs.none { it.id == pack.id } shouldBe true
        val l = lobby().apply { updateSets(listOf("J22")) }
        l.useJumpstart = false
        l.isJumpstart shouldBe false
        l.useJumpstart = true
        l.updateSets(listOf("JMP", "J22")) shouldBe true
        l.isJumpstart shouldBe false
    }

})
