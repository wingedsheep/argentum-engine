package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zurgo and Ojutai (MOM #258) — "Whenever one or more Dragons you control deal combat damage to a
 * player or battle, look at the top three cards of your library. Put one of them into your hand and
 * the rest on the bottom of your library in any order. You may return one of those Dragons to its
 * owner's hand."
 *
 * Per the ruling it triggers once per player *and* once per battle dealt combat damage by your
 * Dragons, however many Dragons hit each. Invasion of Innistrad (defense 5) is the player's own
 * Siege, protected by the opponent, so it is a battle the player may attack.
 */
class ZurgoAndOjutaiScenarioTest : ScenarioTestBase() {

    /** Names offered by the last "return one of those Dragons" choice. */
    private var returnOptions: List<String> = emptyList()

    private fun board(vararg extraAttackers: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Zurgo and Ojutai", summoningSickness = false)
        .apply { extraAttackers.forEach { withCardOnBattlefield(1, it, summoningSickness = false) } }
        .withCardOnBattlefield(1, "Invasion of Innistrad")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    private fun TestGame.siegeDefense(): Int {
        val siege = findPermanent("Invasion of Innistrad")!!
        return state.getEntity(siege)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) ?: 0
    }

    /**
     * Resolves every Zurgo trigger on the stack: keeps the first card looked at, and answers the
     * "return one of those Dragons" choice with [returnDragon] (or declines). Returns how many
     * look-at-top choices were made — one per resolved trigger.
     */
    private fun TestGame.resolveTriggers(returnDragon: String? = null): Int {
        var looks = 0
        var guard = 0
        while (guard++ < 40) {
            val decision = state.pendingDecision
            when {
                decision is SelectCardsDecision && decision.prompt.contains("Dragons") -> {
                    returnOptions = decision.options.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
                    val pick = returnDragon?.let { name ->
                        decision.options.filter { state.getEntity(it)?.get<CardComponent>()?.name == name }
                    }.orEmpty()
                    selectCards(pick).error shouldBe null
                }
                decision is SelectCardsDecision -> {
                    looks++
                    selectCards(decision.options.take(decision.minSelections.coerceAtLeast(1))).error shouldBe null
                }
                decision is ReorderLibraryDecision -> keepLibraryOrder().error shouldBe null
                // A defeated Siege's "you may cast it transformed" — decline.
                decision is YesNoDecision -> answerYesNo(false).error shouldBe null
                decision != null -> error("unexpected decision: $decision")
                state.stack.isNotEmpty() -> resolveStack()
                else -> return looks
            }
        }
        error("did not settle")
    }

    private fun TestGame.attack(players: Map<String, Int>, battles: Map<String, String> = emptyMap()) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(playerAttackers = players, permanentAttackers = battles)
            .error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    init {
        test("triggers once for the player and once for the battle its Dragons hit") {
            val game = board("Pearl Dragon")
            val handBefore = game.handSize(1)

            game.attack(
                players = mapOf("Zurgo and Ojutai" to 2),
                battles = mapOf("Pearl Dragon" to "Invasion of Innistrad")
            )

            withClue("both Dragons connected") {
                game.getLifeTotal(2) shouldBe 16
                game.siegeDefense() shouldBe 1
            }
            withClue("one trigger for the opponent, one for the battle") {
                game.resolveTriggers() shouldBe 2
            }
            game.handSize(1) shouldBe handBefore + 2
        }

        test("several Dragons hitting the same player trigger it once") {
            val game = board("Pearl Dragon")
            game.attack(players = mapOf("Zurgo and Ojutai" to 2, "Pearl Dragon" to 2))
            game.getLifeTotal(2) shouldBe 12
            game.resolveTriggers() shouldBe 1
        }

        test("a battle the damage defeats still triggers it") {
            val game = board("Pearl Dragon")
            game.attack(
                players = emptyMap(),
                battles = mapOf("Zurgo and Ojutai" to "Invasion of Innistrad", "Pearl Dragon" to "Invasion of Innistrad")
            )
            withClue("8 damage removed every defense counter") { game.siegeDefense() shouldBe 0 }
            game.resolveTriggers(returnDragon = "Pearl Dragon") shouldBe 1
            withClue("the defeated Siege left the battlefield") {
                game.isOnBattlefield("Invasion of Innistrad") shouldBe false
            }
            withClue("both Dragons hit the battle, so both are offered") {
                returnOptions.sorted() shouldBe listOf("Pearl Dragon", "Zurgo and Ojutai")
            }
        }

        test("a non-Dragon connecting does not trigger it") {
            val game = board("Grizzly Bears")
            game.attack(
                players = mapOf("Grizzly Bears" to 2),
                battles = mapOf("Zurgo and Ojutai" to "Invasion of Innistrad")
            )
            game.getLifeTotal(2) shouldBe 18
            withClue("only Zurgo's hit on the battle triggers") {
                game.resolveTriggers() shouldBe 1
            }
        }

        test("may return one of the Dragons that dealt the damage to its owner's hand") {
            val game = board("Pearl Dragon")
            game.attack(players = mapOf("Pearl Dragon" to 2))
            game.resolveTriggers(returnDragon = "Pearl Dragon") shouldBe 1
            withClue("only the Dragon that dealt the damage is offered") {
                returnOptions shouldBe listOf("Pearl Dragon")
            }
            withClue("Pearl Dragon went back to hand; Zurgo, which dealt no damage, stayed") {
                game.isOnBattlefield("Pearl Dragon") shouldBe false
                game.isInHand(1, "Pearl Dragon") shouldBe true
                game.isOnBattlefield("Zurgo and Ojutai") shouldBe true
            }
        }

        test("has hexproof as long as it entered this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Zurgo and Ojutai")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Pearl Dragon", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Zurgo and Ojutai").error shouldBe null
            game.resolveStack()
            val zurgo = game.findPermanent("Zurgo and Ojutai")!!
            val pearl = game.findPermanent("Pearl Dragon")!!
            withClue("it entered this turn") {
                game.state.projectedState.hasKeyword(zurgo, Keyword.HEXPROOF) shouldBe true
            }
            withClue("the grant is to itself only") {
                game.state.projectedState.hasKeyword(pearl, Keyword.HEXPROOF) shouldBe false
            }
        }

        test("has no hexproof when it didn't enter this turn") {
            val game = board()
            val zurgo = game.findPermanent("Zurgo and Ojutai")!!
            game.state.projectedState.hasKeyword(zurgo, Keyword.HEXPROOF) shouldBe false
        }
    }
}
