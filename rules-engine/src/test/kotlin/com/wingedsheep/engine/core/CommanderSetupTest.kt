package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phase 1.1 + 1.2 — Format wired through GameInitializer, commander entity routed to Zone.COMMAND.
 */
class CommanderSetupTest : FunSpec({

    fun registry(): CardRegistry {
        val r = CardRegistry()
        r.register(TestCards.all)
        return r
    }

    test("default format is Standard with 20 life and library-only zone setup") {
        val initializer = GameInitializer(registry())
        val result = initializer.initializeGame(
            GameConfig(
                players = listOf(
                    PlayerConfig("P1", Deck.of("Forest" to 60)),
                    PlayerConfig("P2", Deck.of("Forest" to 60)),
                ),
                skipMulligans = true,
            )
        )

        result.state.format shouldBe Format.Standard

        for (pid in result.playerIds) {
            result.state.getEntity(pid)!!.get<LifeTotalComponent>()!!.life shouldBe 20
            result.state.getZone(ZoneKey(pid, Zone.COMMAND)).size shouldBe 0
            result.state.getEntity(pid)!!.get<CommanderRegistryComponent>() shouldBe null
        }
    }

    test("Commander format starts both players at 40 life with their commander in the command zone") {
        val initializer = GameInitializer(registry())
        // Deck.cards does NOT include the commander (CR 903.6a / DeckValidator convention) —
        // the commander is instantiated separately by GameInitializer and routed to Zone.COMMAND.
        val deck = Deck.of("Forest" to 99)
        val result = initializer.initializeGame(
            GameConfig(
                format = Format.Commander(),
                players = listOf(
                    PlayerConfig("P1", deck, commanderCardName = "Test Hasty Prospector"),
                    PlayerConfig("P2", deck, commanderCardName = "Test Hasty Prospector"),
                ),
                skipMulligans = true,
            )
        )

        (result.state.format is Format.Commander) shouldBe true

        for (pid in result.playerIds) {
            // 40 life
            result.state.getEntity(pid)!!.get<LifeTotalComponent>()!!.life shouldBe 40

            // Exactly one entity in command zone, and it's the registered commander
            val commandZone = result.state.getZone(ZoneKey(pid, Zone.COMMAND))
            commandZone.size shouldBe 1
            val cmdrId = commandZone.single()

            val cmdr = result.state.getEntity(cmdrId)!!
            cmdr.get<CardComponent>()!!.name shouldBe "Test Hasty Prospector"
            cmdr.get<CommanderComponent>() shouldNotBe null
            cmdr.get<CommanderComponent>()!!.ownerId shouldBe pid
            cmdr.get<CommanderComponent>()!!.castsFromCommandZone shouldBe 0

            // Player carries CommanderRegistryComponent listing that entity
            val registry = result.state.getEntity(pid)!!.get<CommanderRegistryComponent>()
            registry shouldNotBe null
            registry!!.commanderIds shouldContain cmdrId

            // Commander is NOT in the library
            result.state.getZone(ZoneKey(pid, Zone.LIBRARY)) shouldNotBe commandZone
            (cmdrId in result.state.getZone(ZoneKey(pid, Zone.LIBRARY))) shouldBe false
        }
    }

    test("Team vs Team can enable Commander rules for every player") {
        val initializer = GameInitializer(registry())
        val deck = Deck.of("Forest" to 99)
        val format = Format.TeamVsTeam(
            startingLife = 40,
            commanderDamageThreshold = 21,
            deckSize = 100,
        )
        val result = initializer.initializeGame(
            GameConfig(
                format = format,
                players = (1..4).map { seat ->
                    PlayerConfig("P$seat", deck, commanderCardName = "Test Hasty Prospector")
                },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                skipMulligans = true,
            )
        )

        result.state.format shouldBe format
        for (pid in result.playerIds) {
            result.state.getEntity(pid)!!.get<LifeTotalComponent>()!!.life shouldBe 40
            val commanderId = result.state.getZone(ZoneKey(pid, Zone.COMMAND)).single()
            result.state.getEntity(commanderId)!!.get<CommanderComponent>()!!.ownerId shouldBe pid
            (commanderId in result.state.getZone(ZoneKey(pid, Zone.LIBRARY))) shouldBe false
        }
    }

    test("Commander format rejects players without a designated commander") {
        val initializer = GameInitializer(registry())
        // Deck.cards does NOT include the commander (CR 903.6a / DeckValidator convention) —
        // the commander is instantiated separately by GameInitializer and routed to Zone.COMMAND.
        val deck = Deck.of("Forest" to 99)

        shouldThrow<IllegalArgumentException> {
            initializer.initializeGame(
                GameConfig(
                    format = Format.Commander(),
                    players = listOf(
                        PlayerConfig("P1", deck, commanderCardName = "Test Hasty Prospector"),
                        PlayerConfig("P2", deck, commanderCardName = null),
                    ),
                    skipMulligans = true,
                )
            )
        }
    }

    test("Commander format rejects a commander name unknown to the card registry") {
        val initializer = GameInitializer(registry())
        val deck = Deck.of("Forest" to 99)

        shouldThrow<IllegalArgumentException> {
            initializer.initializeGame(
                GameConfig(
                    format = Format.Commander(),
                    players = listOf(
                        PlayerConfig("P1", deck, commanderCardName = "Definitely Not A Real Card 12345"),
                        PlayerConfig("P2", deck, commanderCardName = "Definitely Not A Real Card 12345"),
                    ),
                    skipMulligans = true,
                )
            )
        }
    }
})
