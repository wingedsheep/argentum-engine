package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vorinclex, Monstrous Raider (KHM #199).
 *
 * "If you would put one or more counters on a permanent or player, put twice that many … If an
 * opponent would put one or more counters …, they put half that many …, rounded down." Which clause
 * applies depends on who puts the counters — not on who controls the permanent — and a permanent's
 * entry counters are put on it by its controller (the rulings).
 */
class VorinclexMonstrousRaiderScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    // "Target player gets a poison counter." — a counter put on a player.
    private val venom = card("Test Venom") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val victim = target(Targets.Player)
            effect = Effects.AddCounters(CounterType.POISON, 1, victim)
        }
    }

    init {
        cardRegistry.register(venom)

        test("a poison counter you give a player is doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardInHand(1, "Test Venom")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val venomId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Venom"
            }
            game.execute(CastSpell(game.player1Id, venomId, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.resolveStack()

            counters(game, game.player2Id, CounterType.POISON) shouldBe 2
        }

        test("a stolen Vorinclex works for its new controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Act of Treason")
                .withCardInHand(2, "Battlegrowth")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vorinclex = game.findPermanent("Vorinclex, Monstrous Raider")!!
            game.castSpell(2, "Act of Treason", targetId = vorinclex).error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Battlegrowth", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("the thief is now \"you\": their counter doubles") {
                counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }
        }

        test("an opponent's +1 loyalty cost adds nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardOnBattlefield(2, "Tyvar Kell")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tyvar = game.findPermanent("Tyvar Kell")!!
            val before = counters(game, tyvar, CounterType.LOYALTY)
            val plusOne = cardRegistry.getCard("Tyvar Kell")!!.script.activatedAbilities[0]
            game.execute(ActivateAbility(game.player2Id, tyvar, plusOne.id, emptyList())).error shouldBe null

            withClue("half of one loyalty counter, rounded down, is none") {
                counters(game, tyvar, CounterType.LOYALTY) shouldBe before
            }
        }

        test("counters you put are doubled, on an opponent's creature too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Battlegrowth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Battlegrowth", targetId = bears).error shouldBe null
            game.resolveStack()

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("an opponent's single counter is halved to none, even on their own creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Battlegrowth")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Battlegrowth", targetId = bears).error shouldBe null
            game.resolveStack()

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        }

        test("the opponent orders their own Hardened Scales before the halving") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardOnBattlefield(2, "Hardened Scales")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Battlegrowth")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Battlegrowth", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("(1 + 1) / 2 = 1, rather than halving first and ending the placement") {
                counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }

        test("a planeswalker's entry loyalty doubles for you and halves for an opponent") {
            val mine = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardInHand(1, "Tyvar Kell")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            mine.castSpell(1, "Tyvar Kell").error shouldBe null
            mine.resolveStack()
            withClue("3 loyalty doubled") {
                counters(mine, mine.findPermanent("Tyvar Kell")!!, CounterType.LOYALTY) shouldBe 6
            }

            val theirs = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vorinclex, Monstrous Raider")
                .withCardInHand(2, "Tyvar Kell")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            theirs.castSpell(2, "Tyvar Kell").error shouldBe null
            theirs.resolveStack()
            withClue("3 loyalty halved, rounded down") {
                counters(theirs, theirs.findPermanent("Tyvar Kell")!!, CounterType.LOYALTY) shouldBe 1
            }
        }
    }
}
