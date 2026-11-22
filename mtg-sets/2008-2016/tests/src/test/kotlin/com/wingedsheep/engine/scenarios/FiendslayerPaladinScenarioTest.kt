package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fiendslayer Paladin — {1}{W}{W} Creature — Human Knight 2/2 (M14 #18).
 *
 * "First strike. Lifelink. This creature can't be the target of black or red spells your
 * opponents control."
 *
 * Only *spells*, only black or red ones, only the opponents': its controller's own black or red
 * spells, opponents' spells of other colours and abilities from black or red sources may all
 * still target it, and a black or red spell that doesn't target still affects it (the ruling).
 */
class FiendslayerPaladinScenarioTest : ScenarioTestBase() {

    private fun opponentHolding(vararg spells: String): TestGame {
        val builder = scenario()
            .withPlayers("Paladin", "Opponent")
            .withCardOnBattlefield(1, "Fiendslayer Paladin")
            .withLandsOnBattlefield(2, "Island", 1)
            .withLandsOnBattlefield(2, "Swamp", 2)
            .withLandsOnBattlefield(2, "Mountain", 2)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        spells.forEach { builder.withCardInHand(2, it) }
        return builder.build()
    }

    private fun TestGame.targetsOffered(player: Int, spell: String): List<EntityId> {
        return getLegalActions(player).firstOrNull { it.description.contains(spell) }?.let { action ->
            action.targetRequirements?.firstOrNull()?.validTargets ?: action.validTargets.orEmpty()
        }.orEmpty()
    }

    init {
        test("an opponent's black or red spell can't target it") {
            val game = opponentHolding("Doom Blade", "Shock")
            val paladin = game.findPermanent("Fiendslayer Paladin")!!

            withClue("neither spell is offered the Paladin") {
                game.targetsOffered(2, "Doom Blade") shouldNotContain paladin
                game.targetsOffered(2, "Shock") shouldNotContain paladin
            }
            game.castSpell(2, "Doom Blade", paladin).error shouldNotBe null
            game.castSpell(2, "Shock", paladin).error shouldNotBe null
            game.isOnBattlefield("Fiendslayer Paladin") shouldBe true
        }

        test("an opponent's spell of another colour may target it") {
            val game = opponentHolding("Unsummon")
            val paladin = game.findPermanent("Fiendslayer Paladin")!!

            game.targetsOffered(2, "Unsummon") shouldContain paladin
            game.castSpell(2, "Unsummon", paladin).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Fiendslayer Paladin") shouldBe true
        }

        test("an ability from a red source may target it") {
            val game = scenario()
                .withPlayers("Paladin", "Opponent")
                .withCardOnBattlefield(1, "Fiendslayer Paladin")
                .withCardOnBattlefield(2, "Prodigal Pyromancer")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val paladin = game.findPermanent("Fiendslayer Paladin")!!
            val pyromancer = game.findPermanent("Prodigal Pyromancer")!!
            val abilityId = cardRegistry.getCard("Prodigal Pyromancer")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player2Id,
                    sourceId = pyromancer,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, paladin))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.getEntity(paladin)?.get<DamageComponent>().shouldNotBeNull().amount shouldBe 1
        }

        test("its controller's own red spell may target it") {
            val game = scenario()
                .withPlayers("Paladin", "Opponent")
                .withCardOnBattlefield(1, "Fiendslayer Paladin")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val paladin = game.findPermanent("Fiendslayer Paladin")!!

            game.targetsOffered(1, "Shock") shouldContain paladin
            game.castSpell(1, "Shock", paladin).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Fiendslayer Paladin") shouldBe false
        }

        test("a red spell that doesn't target still affects it") {
            val game = scenario()
                .withPlayers("Paladin", "Opponent")
                .withCardOnBattlefield(1, "Fiendslayer Paladin")
                .withCardInHand(2, "Pyroclasm")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Pyroclasm").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Fiendslayer Paladin") shouldBe false
        }
    }
}
