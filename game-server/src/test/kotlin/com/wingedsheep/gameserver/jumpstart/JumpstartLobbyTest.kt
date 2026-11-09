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
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
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

    test("one pack from each offer makes an exact forty-card deck without adding basic lands") {
        val l = lobby()
        l.startJumpstart(guest) shouldBe false
        l.startJumpstart(host) shouldBe true
        l.startJumpstart(host) shouldBe false
        for (id in listOf(host, guest)) {
            val player = l.players.getValue(id)
            player.jumpstartOffers.size shouldBe 3
            player.jumpstartSecondOffers.size shouldBe 3
            // Both rows are sent up front, so the pair can be weighed together.
            l.buildLobbyUpdate(id).jumpstart!!.secondOffers.map { it.id } shouldBe player.jumpstartSecondOffers
            val first = player.jumpstartOffers.first()
            val second = player.jumpstartSecondOffers.last()
            l.pickJumpstart(id, "unoffered", second) shouldBe false
            l.pickJumpstart(id, first, "unoffered") shouldBe false
            // Each pack must come from its own row (unless the same id was rolled in both).
            if (second !in player.jumpstartOffers) l.pickJumpstart(id, second, first) shouldBe false
            l.pickJumpstart(id, first, second) shouldBe true
            l.pickJumpstart(id, first, second) shouldBe false
            l.buildLobbyUpdate(id).jumpstart!!.selected.map { it.id to it.cards.size } shouldBe listOf(first to 20, second to 20)
            val pool = l.players.getValue(id).cardPool
            pool.size shouldBe 40
            val expected = (JumpstartPacks.lists.getValue(first) + JumpstartPacks.lists.getValue(second)).groupingBy { it }.eachCount()
            pool.groupingBy { it.name }.eachCount() shouldBe expected
            l.submitDeck(id, expected + ("Plains" to 50)).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Error>()
            l.submitDeck(id, expected).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Success>()
            l.getSubmittedSideboard(id) shouldBe emptyMap()
            l.unsubmitDeck(id) shouldBe false
        }
        l.allDecksSubmitted() shouldBe true
    }

    test("a lobby persisted mid-pick under one-at-a-time offers returns to a choice of both") {
        val l = lobby()
        l.startJumpstart(host) shouldBe true
        val firstOffers = l.players.getValue(host).jumpstartOffers
        val secondOffers = l.players.getValue(host).jumpstartSecondOffers
        val first = JumpstartPacks(generator).packs.first { it.id == firstOffers.first() }
        val legacy = l.toPersistent().let { persisted ->
            persisted.copy(players = persisted.players.mapValues { (id, player) ->
                when (id) {
                    // Picked one pack: offers had moved on to round two.
                    host.value -> player.copy(jumpstartSelections = listOf(first.id), jumpstartFirstOffers = firstOffers,
                        jumpstartOffers = secondOffers, jumpstartSecondOffers = emptyList(),
                        cardPoolNames = first.cards.map { it.name })
                    // Picked nothing: round two hadn't been rolled.
                    else -> player.copy(jumpstartSecondOffers = emptyList())
                }
            })
        }
        val (restored, _) = restoreTournamentLobby(legacy, registry, generator)
        restored.players.getValue(host).let {
            it.jumpstartSelections shouldBe emptyList()
            it.cardPool shouldBe emptyList()
            it.jumpstartOffers shouldBe firstOffers
            it.jumpstartSecondOffers shouldBe secondOffers
        }
        restored.players.getValue(guest).jumpstartSecondOffers.size shouldBe 3
        restored.pickJumpstart(host, firstOffers.first(), secondOffers.first()) shouldBe true
        restored.players.getValue(host).cardPool.size shouldBe 40
    }

    test("every published pack makes a submittable deck, including seven Snow-Covered Islands") {
        for (set in JumpstartPacks.SUPPORTED_SETS) {
            val packs = JumpstartPacks(generator, set).packs
            for (pack in packs) {
                val l = lobby().apply { updateSets(listOf(set)) }
                l.startJumpstart(host) shouldBe true
                l.players[host] = l.players.getValue(host).copy(
                    jumpstartOffers = listOf(pack.id), jumpstartSecondOffers = listOf(packs.first().id))
                l.pickJumpstart(host, pack.id, packs.first().id) shouldBe true
                val deck = l.players.getValue(host).cardPool.groupingBy { it.name }.eachCount()
                withClue("$set ${pack.id}") {
                    l.submitDeck(host, deck).shouldBeInstanceOf<TournamentLobby.DeckSubmissionResult.Success>()
                }
            }
        }
    }

    test("private offers and picks survive a server restart") {
        val l = lobby()
        l.startJumpstart(host) shouldBe true
        val (restored, _) = restoreTournamentLobby(l.toPersistent(), registry, generator)
        restored.buildLobbyUpdate(host).jumpstart shouldBe l.buildLobbyUpdate(host).jumpstart
        val player = restored.players.getValue(host)
        restored.pickJumpstart(host, player.jumpstartOffers.first(), player.jumpstartSecondOffers.first()) shouldBe true
        val (again, _) = restoreTournamentLobby(restored.toPersistent(), registry, generator)
        again.buildLobbyUpdate(host).jumpstart shouldBe restored.buildLobbyUpdate(host).jumpstart
        again.players.getValue(host).cardPool.map { it.name } shouldBe restored.players.getValue(host).cardPool.map { it.name }
        again.buildLobbyUpdate(guest).jumpstart!!.selectedPacks shouldBe emptyList()
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
    test("the two rows share no theme when six are playable, and are drawn independently when not") {
        // How many themes are playable depends on which cards are implemented, so the assertion
        // follows the corpus rather than assuming a count.
        for (set in JumpstartPacks.SUPPORTED_SETS) {
            val packs = JumpstartPacks(generator, set)
            val available = packs.available(emptySet()).map { it.theme }.distinct().size
            repeat(20) {
                val (first, second) = packs.offerPair(emptySet())
                val firstThemes = first.map { it.substringBefore(" (") }
                val secondThemes = second.map { it.substringBefore(" (") }
                withClue("$set with $available playable themes: $first / $second") {
                    firstThemes.distinct().size shouldBe minOf(3, available)
                    secondThemes.distinct().size shouldBe minOf(3, available)
                    if (available >= 6) (firstThemes + secondThemes).distinct().size shouldBe 6
                }
            }
        }
    }

    test("J22 has 121 exact published variants spanning 46 themes") {
        JumpstartPacks.listsFor("J22").size shouldBe 121
        JumpstartPacks.listsFor("J22").values.all { it.size == 20 } shouldBe true
        JumpstartPacks.listsFor("J22").keys.map { it.substringBefore(" (") }.distinct().size shouldBe 46
        JumpstartPacks(generator, "J22").packs.map { it.theme }.distinct().size.let { it >= 3 } shouldBe true
    }

    test("J25 has 121 exact published variants spanning 46 themes, and every one is playable") {
        JumpstartPacks.listsFor("J25").size shouldBe 121
        JumpstartPacks.listsFor("J25").values.all { it.size == 20 } shouldBe true
        JumpstartPacks.listsFor("J25").keys.map { it.substringBefore(" (") }.distinct().size shouldBe 46
        // Foundations Jumpstart is fully implemented, so no variant may drop out for a missing card.
        val resolved = JumpstartPacks(generator, "J25").packs.map { it.id }.toSet()
        JumpstartPacks.listsFor("J25").keys.filterNot { it in resolved }.shouldBeEmpty()
        lobby().apply { updateSets(listOf("J25")) }.isJumpstart shouldBe true
    }

    test("switching from JMP to J22 uses J22 offers and restores an exact deck") {
        val l = lobby()
        // Materialize the JMP catalogue first, as the host may do before changing the set.
        l.jumpstartStartError() shouldBe null
        l.updateSets(listOf("J22")) shouldBe true
        l.isJumpstart shouldBe true
        l.startJumpstart(host) shouldBe true
        val lists = JumpstartPacks.listsFor("J22")
        val player = l.players.getValue(host)
        (player.jumpstartOffers + player.jumpstartSecondOffers).all { it in lists } shouldBe true
        val first = player.jumpstartOffers.first()
        val second = player.jumpstartSecondOffers.first()
        val (restored, _) = restoreTournamentLobby(l.toPersistent(), registry, generator)
        restored.buildLobbyUpdate(host).jumpstart shouldBe l.buildLobbyUpdate(host).jumpstart
        restored.pickJumpstart(host, first, second) shouldBe true
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
