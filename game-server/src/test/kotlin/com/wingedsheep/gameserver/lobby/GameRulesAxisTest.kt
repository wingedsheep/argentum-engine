package com.wingedsheep.gameserver.lobby

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.persistence.restoreTournamentLobby
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.GameRules
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The Rules axis: one field, one authority, and one statement of the rule that limits it.
 *
 * "Does this game run Commander rules?" used to be a disjunction over two unrelated fields — a
 * `PREMADE_DECKS` lobby with a commander-shaped `deckFormat`, or a Commander pack format — written
 * out at four call sites with three more spellings elsewhere. The copies could not see each other,
 * which is how the client came to gate Commander pods on a check that structurally could not observe
 * the premade path. [TournamentLobby.usesCommanderRules] replaces all of them, so what needs pinning
 * is that it stays true for every way of reaching Commander, survives a restart, and refuses exactly
 * one table.
 */
class GameRulesAxisTest : FunSpec({

    val forest = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val generator = BoosterGenerator(
        mapOf(
            "TST" to BoosterGenerator.SetConfig(
                setCode = "TST",
                setName = "Test",
                cards = emptyList(),
                basicLands = listOf(forest),
            )
        )
    )

    fun lobby(
        format: TournamentFormat = TournamentFormat.PREMADE_DECKS,
        rules: GameRules = GameRules.STANDARD,
        gameMode: LobbyGameMode = LobbyGameMode.TOURNAMENT,
        deckFormat: DeckFormat? = null,
    ): TournamentLobby = TournamentLobby(
        setCodes = listOf("TST"),
        setNames = listOf("Test"),
        boosterGenerator = generator,
        format = format,
        rules = rules,
        gameMode = gameMode,
        deckFormat = deckFormat,
    )

    test("one authority answers for a drafted, a sealed and a brought Commander deck alike") {
        // The point of the axis: the answer no longer depends on where the cards came from, so a
        // consumer needs one branch where it used to need three.
        for (format in TournamentFormat.entries) {
            lobby(format = format, rules = GameRules.COMMANDER).usesCommanderRules shouldBe true
            lobby(format = format, rules = GameRules.STANDARD).usesCommanderRules shouldBe false
        }
    }

    test("the pack shape and commander deck legality only infer it, they are not it") {
        // Both used to *be* the answer. They are now inputs to one inference, used for older clients
        // and older persisted lobbies — so a host who explicitly picks Standard rules on a Commander
        // Draft lobby gets Standard, which the old disjunction made impossible to express.
        GameRules.inferred(commanderPackShape = true, deckFormat = null) shouldBe GameRules.COMMANDER
        GameRules.inferred(commanderPackShape = false, deckFormat = DeckFormat.COMMANDER) shouldBe GameRules.COMMANDER
        GameRules.inferred(commanderPackShape = false, deckFormat = DeckFormat.BRAWL) shouldBe GameRules.COMMANDER
        GameRules.inferred(commanderPackShape = false, deckFormat = DeckFormat.STANDARD_BRAWL) shouldBe GameRules.COMMANDER
        GameRules.inferred(commanderPackShape = false, deckFormat = DeckFormat.MODERN) shouldBe GameRules.STANDARD
        GameRules.inferred(commanderPackShape = false, deckFormat = null) shouldBe GameRules.STANDARD

        lobby(format = TournamentFormat.COMMANDER_DRAFT, rules = GameRules.STANDARD)
            .usesCommanderRules shouldBe false
    }

    test("Two-Headed Giant is the one table Commander rules cannot have") {
        for (mode in LobbyGameMode.entries) {
            val conflict = lobby(rules = GameRules.COMMANDER, gameMode = mode).rulesTableConflict
            if (mode == LobbyGameMode.TWO_HEADED_GIANT) {
                // CR 810.4: the team shares one life total, so there is nowhere to put Commander's 40.
                conflict shouldContain "Two-Headed Giant"
                conflict shouldContain "Commander"
            } else {
                conflict.shouldBeNull()
            }
            // Standard rules sit at every table, so the conflict is about Commander and not about 2HG.
            lobby(rules = GameRules.STANDARD, gameMode = mode).rulesTableConflict.shouldBeNull()
        }
    }

    test("the rules survive a persistence round-trip") {
        // Without this the axis would be lost on a server restart, and a pod that was mid-way through
        // deckbuilding would come back as a Standard game demanding no commanders.
        val registry = CardRegistry().also { it.register(listOf(forest)) }
        for (rules in GameRules.entries) {
            val restored = restoreTournamentLobby(
                lobby(format = TournamentFormat.SEALED, rules = rules).toPersistent(),
                registry,
                generator,
            ).first
            restored.rules shouldBe rules
        }
    }

    test("a persisted lobby from before the axis infers its rules from the pack shape") {
        // Rows written by the previous version carry no `rules` at all (null, not "STANDARD" — the
        // distinction is what lets an explicit Standard choice survive above).
        val registry = CardRegistry().also { it.register(listOf(forest)) }
        val legacyCommander = lobby(format = TournamentFormat.COMMANDER_SEALED, rules = GameRules.COMMANDER)
            .toPersistent()
            .copy(rules = null)
        restoreTournamentLobby(legacyCommander, registry, generator).first
            .usesCommanderRules shouldBe true

        val legacyStandard = lobby(format = TournamentFormat.SEALED).toPersistent().copy(rules = null)
        restoreTournamentLobby(legacyStandard, registry, generator).first
            .usesCommanderRules shouldBe false
    }
})
