package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Equinox — {W} Enchantment — Aura. Enchant land.
 * Enchanted land has "{T}: Counter target spell if it would destroy a land you control."
 *
 * Rulings pinned: the ability can target any spell; it counters only a spell that, resolving right
 * then, would itself destroy a land its controller controls — not one aimed at someone else's land,
 * not one whose chosen target isn't a land, and not one that makes you sacrifice lands.
 */
class EquinoxScenarioTest : ScenarioTestBase() {

    private val equinoxAbilityId by lazy {
        cardRegistry.getCard("Equinox")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().first().ability.id
    }

    /** Player 1 controls an Equinox-enchanted Plains, an Island and an Ornithopter; player 2 is active. */
    private fun board(spell: String, lands: String, landCount: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardAttachedTo(1, "Equinox", "Plains")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(1, "Ornithopter")
        .withLandsOnBattlefield(2, lands, landCount)
        .withCardInHand(2, spell)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.spellOnStack(name: String): EntityId =
        state.stack.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** Player 2 passes with [spell] on the stack; player 1 taps the enchanted Plains targeting it. */
    private fun TestGame.respondWithEquinox(spell: String) {
        passPriority()
        val plains = findPermanent("Plains")!!
        val result = execute(
            ActivateAbility(player1Id, plains, equinoxAbilityId, listOf(ChosenTarget.Spell(spellOnStack(spell))))
        )
        withClue("Equinox's ability can target $spell: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Equinox") {
            test("counters a spell that would destroy a land you control") {
                val game = board("Stone Rain", "Mountain", 3)
                game.castSpell(2, "Stone Rain", game.findPermanent("Island")!!).error shouldBe null
                game.respondWithEquinox("Stone Rain")

                game.isOnBattlefield("Island") shouldBe true
                game.isInGraveyard(2, "Stone Rain") shouldBe true
            }

            test("counters a sweep that would destroy every land") {
                val game = board("Armageddon", "Plains", 4)
                game.castSpell(2, "Armageddon").error shouldBe null
                game.respondWithEquinox("Armageddon")

                game.findPermanents("Plains").size shouldBe 5
                game.isOnBattlefield("Island") shouldBe true
                game.isInGraveyard(2, "Armageddon") shouldBe true
            }

            test("doesn't counter a spell aimed at another player's land") {
                val game = board("Stone Rain", "Mountain", 3)
                val ownMountain = game.findPermanents("Mountain").first()
                game.castSpell(2, "Stone Rain", ownMountain).error shouldBe null
                game.respondWithEquinox("Stone Rain")

                game.findPermanents("Mountain").size shouldBe 2
                game.isOnBattlefield("Island") shouldBe true
            }

            test("doesn't counter 'destroy target artifact or land' aimed at an artifact") {
                val game = board("Pillage", "Mountain", 4)
                game.castSpell(2, "Pillage", game.findPermanent("Ornithopter")!!).error shouldBe null
                game.respondWithEquinox("Pillage")

                game.isInGraveyard(1, "Ornithopter") shouldBe true
            }

            test("doesn't counter a spell that makes you sacrifice lands") {
                val game = board("Wildfire", "Mountain", 6)
                game.castSpell(2, "Wildfire").error shouldBe null
                game.respondWithEquinox("Wildfire")
                // Player 2 has more than four lands, so they choose which four to sacrifice.
                while (game.hasPendingDecision()) {
                    val choice = game.getPendingDecision() as SelectCardsDecision
                    game.selectCards(choice.options.take(choice.minSelections))
                    game.resolveStack()
                }

                withClue("Wildfire resolved: player 1 sacrificed both lands") {
                    game.isOnBattlefield("Island") shouldBe false
                    game.findPermanents("Plains").size shouldBe 0
                }
            }

            test("can target a spell that would destroy nothing, without countering it") {
                val game = board("Lightning Bolt", "Mountain", 1)
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Ornithopter")!!).error shouldBe null
                game.respondWithEquinox("Lightning Bolt")

                game.isInGraveyard(1, "Ornithopter") shouldBe true
            }
        }
    }
}
