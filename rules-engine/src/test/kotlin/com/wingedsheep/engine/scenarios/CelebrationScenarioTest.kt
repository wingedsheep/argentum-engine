package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the **Celebration** ability word (Wilds of Eldraine).
 *
 * Celebration is an ability word (CR 207.2c) — italic flavor with no rules meaning — so the whole
 * mechanic is one condition: "two or more nonland permanents entered the battlefield under your
 * control this turn", modelled as
 * `Compare(TurnTracking(You, NONLAND_PERMANENTS_ENTERED), GTE, Fixed(2))` behind
 * `Conditions.Celebration`, over the per-player
 * [PermanentsEnteredUnderControlThisTurnComponent] entry log.
 *
 * Each test below pins a clause of the WOE release-notes rulings:
 *  - "Celebration abilities only care if **two or more** … They won't get more powerful if more
 *    than two permanents entered." — threshold, no scaling.
 *  - "The permanents that entered **don't need to remain on the battlefield or under your
 *    control**. Celebration abilities are checking for past events, not the current game state."
 *  - "**nonland** permanents" — lands (including land creatures) never count; tokens do.
 *  - Both shipped shapes: the intervening-'if' trigger (CR 603.4, Pests of Honor) and the
 *    "as long as" conditional static (Armory Mice).
 *
 * A permanent that leaves and re-enters is a new object (CR 400.7), so its second entry is a
 * second entry event and counts again.
 */
class CelebrationScenarioTest : ScenarioTestBase() {

    private fun nonlandEntries(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)
            ?.get<PermanentsEnteredUnderControlThisTurnComponent>()
            ?.countNonland() ?: 0

    private fun toughnessOf(game: TestGame, entityId: EntityId): Int =
        game.state.projectedState.getToughness(entityId)!!

    private fun plusOneCounters(game: TestGame, entityId: EntityId): Int =
        game.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun cardInHand(game: TestGame, playerId: EntityId, name: String): EntityId =
        game.state.getHand(playerId).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    /** Overwrite the entry log wholesale — the cheapest way to pin a snapshot shape. */
    private fun setEntryLog(game: TestGame, playerId: EntityId, vararg types: Set<CardType>) {
        game.state = game.state.updateEntity(playerId) { container ->
            container.with(
                PermanentsEnteredUnderControlThisTurnComponent(
                    types.mapIndexed { i, t -> EnteredPermanentRecord(EntityId.of("entry-$i"), t) }
                )
            )
        }
    }

    init {
        // A one-mana vanilla creature, so a test can cast several in a turn without mana or
        // colour-identity noise.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Beast",
                manaCost = ManaCost.parse("{G}"),
                subtypes = setOf(Subtype.BEAST),
                power = 1,
                toughness = 1
            )
        )

        context("Celebration — the entry count itself") {

            test("only nonland permanents count; lands never do") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id
                game.execute(PlayLand(player, cardInHand(game, player, "Forest"))).error shouldBe null
                withClue("a land ETB must not feed the nonland count") {
                    nonlandEntries(game, player) shouldBe 0
                }

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                nonlandEntries(game, player) shouldBe 1
            }

            test("a permanent that is both a land and a creature does not count") {
                // "Nonland permanent" is a type test, not a "not only a land" test: a Dryad
                // Arbor-style land creature is a land, so Celebration ignores it. Pinned against
                // the snapshot the tracker records, since the entry is what's classified.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                setEntryLog(
                    game, game.player1Id,
                    setOf(CardType.LAND, CardType.CREATURE),
                    setOf(CardType.LAND, CardType.CREATURE),
                )
                nonlandEntries(game, game.player1Id) shouldBe 0
            }

            test("tokens count — one Raise the Alarm turns Celebration on by itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mice = game.findPermanent("Armory Mice")!!
                toughnessOf(game, mice) shouldBe 1

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                withClue("two 1/1 Soldier tokens are two nonland permanents entering") {
                    nonlandEntries(game, game.player1Id) shouldBe 2
                    toughnessOf(game, mice) shouldBe 3
                }
            }

            test("the count is per-player — an opponent's permanents never help you") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withCardInHand(2, "Raise the Alarm")
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                nonlandEntries(game, game.player2Id) shouldBe 2
                nonlandEntries(game, game.player1Id) shouldBe 0
                withClue("Armory Mice reads its own controller's count") {
                    toughnessOf(game, game.findPermanent("Armory Mice")!!) shouldBe 1
                }
            }

            test("the same permanent entering twice counts twice (CR 400.7 — a new object)") {
                // Cast a creature, bounce it, re-cast it. One card, two entry events.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Beast")
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                nonlandEntries(game, player) shouldBe 1

                game.castSpell(1, "Unsummon", targetId = game.findPermanent("Test Beast")!!)
                    .error shouldBe null
                game.resolveStack()
                withClue("leaving the battlefield does not un-count the first entry") {
                    nonlandEntries(game, player) shouldBe 1
                }

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                withClue("the re-entry is a second entry event") {
                    nonlandEntries(game, player) shouldBe 2
                }
            }

            test("the count is cleared at the turn boundary") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                nonlandEntries(game, game.player1Id) shouldBe 2

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                nonlandEntries(game, game.player1Id) shouldBe 0
            }
        }

        context("Celebration — Armory Mice (conditional static: +0/+2 as long as …)") {

            test("off at zero and at one entry; on at exactly two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withCardInHand(1, "Test Beast")
                    .withCardsInHand(1, "Test Beast", 1)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mice = game.findPermanent("Armory Mice")!!
                withClue("nothing has entered yet") { toughnessOf(game, mice) shouldBe 1 }

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                withClue("one nonland permanent is below the threshold") {
                    toughnessOf(game, mice) shouldBe 1
                }

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                withClue("the second entry switches the static on") {
                    toughnessOf(game, mice) shouldBe 3
                }
            }

            test("a third entry does not stack the bonus") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mice = game.findPermanent("Armory Mice")!!
                setEntryLog(
                    game, game.player1Id,
                    setOf(CardType.CREATURE),
                    setOf(CardType.CREATURE),
                    setOf(CardType.ARTIFACT),
                    setOf(CardType.ENCHANTMENT),
                )
                withClue("Celebration is a threshold, not a count") {
                    toughnessOf(game, mice) shouldBe 3
                }
            }

            test("stays on after the permanents that entered have left — past events, not board state") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withCardInHand(1, "Raise the Alarm")
                    .withCardsInHand(1, "Unsummon", 2)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mice = game.findPermanent("Armory Mice")!!
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                toughnessOf(game, mice) shouldBe 3

                // Bounce both Soldier tokens — they cease to exist entirely.
                repeat(2) {
                    val soldier = game.findPermanent("Soldier Token")!!
                    game.castSpell(1, "Unsummon", targetId = soldier).error shouldBe null
                    game.resolveStack()
                }
                game.findPermanent("Soldier Token") shouldBe null

                withClue("the entries already happened; Celebration doesn't re-read the board") {
                    toughnessOf(game, mice) shouldBe 3
                }
            }

            test("turns off again on the next turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Armory Mice")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mice = game.findPermanent("Armory Mice")!!
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                toughnessOf(game, mice) shouldBe 3

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                withClue("the tracker resets, so the static drops off") {
                    toughnessOf(game, mice) shouldBe 1
                }
            }

            test("the celebrating permanent's own entry counts toward its condition") {
                // Armory Mice is itself a nonland permanent: cast it as the second entry of the
                // turn and it comes down already buffed.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Test Beast")
                    .withCardInHand(1, "Armory Mice")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Armory Mice").error shouldBe null
                game.resolveStack()

                nonlandEntries(game, game.player1Id) shouldBe 2
                toughnessOf(game, game.findPermanent("Armory Mice")!!) shouldBe 3
            }
        }

        context("Celebration — Pests of Honor (intervening-'if' trigger, CR 603.4)") {

            test("does not trigger when fewer than two nonland permanents entered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pests of Honor")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                nonlandEntries(game, game.player1Id) shouldBe 1

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("the intervening-'if' fails at trigger time, so nothing goes on the stack") {
                    plusOneCounters(game, game.findPermanent("Pests of Honor")!!) shouldBe 0
                }
            }

            test("triggers at the beginning of combat once two entered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pests of Honor")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                nonlandEntries(game, game.player1Id) shouldBe 2

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                plusOneCounters(game, game.findPermanent("Pests of Honor")!!) shouldBe 1
            }

            test("lands entering do not turn the trigger on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pests of Honor")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id
                game.state = game.state.updateEntity(player) { container ->
                    val drops = container
                        .get<com.wingedsheep.engine.state.components.player.LandDropsComponent>()!!
                    container.with(drops.copy(remaining = 2))
                }
                game.execute(PlayLand(player, cardInHand(game, player, "Forest"))).error shouldBe null
                game.execute(PlayLand(player, cardInHand(game, player, "Plains"))).error shouldBe null

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                plusOneCounters(game, game.findPermanent("Pests of Honor")!!) shouldBe 0
            }

            test("triggers again on a later turn once that turn's own entries qualify") {
                // The counter from turn 1 stays, but the tracker resets: turn 2 needs its own
                // two entries before the trigger fires a second time.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pests of Honor")
                    .withCardInHand(1, "Raise the Alarm")
                    .withCardsInHand(1, "Test Beast", 2)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pests = game.findPermanent("Pests of Honor")!!

                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                plusOneCounters(game, pests) shouldBe 1

                // Round through the opponent's main phase back to our own.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                nonlandEntries(game, game.player1Id) shouldBe 0

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("a second, independent Celebration turn adds a second counter") {
                    plusOneCounters(game, pests) shouldBe 2
                }
            }
        }
    }
}
