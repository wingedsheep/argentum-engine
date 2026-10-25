package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `BecomeCreatureEffect.removeSubtypes` — a layer-4 removal of single subtypes that leaves every
 * other type and subtype alone ("it isn't an Equipment", Dancing Sword).
 *
 * Covers: only the named subtype goes (the card type and unrelated subtypes stay), matched without
 * regard to case; it ends with the effect's duration; and the animated former Equipment comes off its
 * host (CR 704.5p — here the creature half of that rule fires too, since `BecomeCreature` always
 * animates).
 */
class BecomeCreatureRemoveSubtypeTest : ScenarioTestBase() {

    /** An Equipment that also carries an unrelated artifact subtype, with a self-animate that sheds Equipment. */
    private val sheddingBlade = card("Test Shedding Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment Clue"
        oracleText = "{0}: Until end of turn, this becomes a 1/1 Construct artifact creature. It isn't an Equipment.\nEquip {1}"
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Construct"),
                removeSubtypes = setOf("equipment"),
                duration = Duration.EndOfTurn,
            )
        }
        equipAbility("{1}")
    }

    init {
        cardRegistry.register(sheddingBlade)

        val animate = sheddingBlade.activatedAbilities.single { !it.isEquipAbility }

        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withCardAttachedTo(1, "Test Shedding Blade", "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.animateBlade() {
            execute(ActivateAbility(player1Id, findPermanent("Test Shedding Blade")!!, animate.id)).error shouldBe null
            resolveStack()
        }

        test("only the named subtype is removed; the card type and other subtypes stay") {
            val game = board()
            game.animateBlade()
            val blade = game.findPermanent("Test Shedding Blade")!!
            val projected = game.state.projectedState

            projected.hasSubtype(blade, "Equipment") shouldBe false
            projected.hasSubtype(blade, "Clue") shouldBe true
            projected.hasSubtype(blade, "Construct") shouldBe true
            projected.hasType(blade, "ARTIFACT") shouldBe true
            projected.isCreature(blade) shouldBe true
        }

        test("an attached Equipment that stops being one comes off its host") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val blade = game.findPermanent("Test Shedding Blade")!!
            game.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bears

            game.animateBlade()

            game.state.getEntity(blade)?.get<AttachedToComponent>() shouldBe null
        }

        test("the subtype comes back when the effect ends") {
            val game = board()
            game.animateBlade()
            val blade = game.findPermanent("Test Shedding Blade")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            withClue("back to a plain Equipment") {
                game.state.projectedState.hasSubtype(blade, "Equipment") shouldBe true
                game.state.projectedState.isCreature(blade) shouldBe false
            }
        }
    }
}
