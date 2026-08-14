package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.DwalinWeaponmaster
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LeoninScimitar
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dwalin, Weaponmaster (HOB #154) — {1}{R/W} Legendary Creature — Dwarf Warrior 2/1, first strike.
 *
 *   Whenever Dwalin enters or attacks, put a hone counter on each Equipment you control.
 *
 * Deliberately tested against **Mirrodin** Equipment, which predate hone by two decades and have no
 * ability referencing it. That is the point of CR 122.1j: the +1/+0 rides on the counter, so Dwalin
 * buffs any Equipment in the deck rather than a curated list of hone cards.
 */
class DwalinWeaponmasterScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? =
        game.state.projectedState.getToughness(id)

    private fun honeCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.HONE) ?: 0

    init {
        cardRegistry.register(DwalinWeaponmaster)
        cardRegistry.register(Bonesplitter)
        cardRegistry.register(LeoninScimitar)

        context("Dwalin, Weaponmaster") {

            test("entering hones every Equipment you control, and only yours") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dwalin, Weaponmaster")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mySplitter = game.findPermanents("Bonesplitter")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                val theirSplitter = game.findPermanents("Bonesplitter")
                    .single { game.state.projectedState.getController(it) == game.player2Id }
                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.castSpell(1, "Dwalin, Weaponmaster").error shouldBe null
                game.resolveStack()

                withClue("each Equipment you control gets exactly one hone counter") {
                    honeCounters(game, mySplitter) shouldBe 1
                    honeCounters(game, scimitar) shouldBe 1
                }
                withClue("the opponent's Equipment is untouched — 'each Equipment you control'") {
                    honeCounters(game, theirSplitter) shouldBe 0
                }
            }

            test("attacking hones again, and the counters pump through a card that never mentions hone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Dwalin, Weaponmaster")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val splitter = game.findPermanent("Bonesplitter")!!

                withClue("before combat: base 2/2 plus Bonesplitter's printed +2/+0") {
                    power(game, bears) shouldBe 4
                    toughness(game, bears) shouldBe 2
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dwalin, Weaponmaster" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger adds a hone counter to the equipped Bonesplitter") {
                    honeCounters(game, splitter) shouldBe 1
                }
                withClue("CR 122.1j stacks the hone bonus on top of Bonesplitter's own +2/+0") {
                    power(game, bears) shouldBe 5
                    toughness(game, bears) shouldBe 2
                }
            }
        }
    }
}
