package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Mistform Stalker.
 *
 * Mistform Stalker: {1}{U}
 * Creature — Illusion
 * 1/1
 * {1}: Mistform Stalker becomes the creature type of your choice until end of turn.
 * {2}{U}{U}: Mistform Stalker gets +2/+2 and gains flying until end of turn.
 */
class MistformStalkerTest : FunSpec({

    val changeTypeAbilityId = AbilityId(UUID.randomUUID().toString())
    val pumpAbilityId = AbilityId(UUID.randomUUID().toString())

    val MistformStalker = CardDefinition(
        name = "Mistform Stalker",
        manaCost = ManaCost.parse("{1}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Illusion"))),
        oracleText = "{1}: Mistform Stalker becomes the creature type of your choice until end of turn.\n{2}{U}{U}: Mistform Stalker gets +2/+2 and gains flying until end of turn.",
        creatureStats = CreatureStats(1, 1),
        script = CardScript.permanent(
            ActivatedAbility(
                id = changeTypeAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{1}")),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.Self
                )
            ),
            ActivatedAbility(
                id = pumpAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{2}{U}{U}")),
                effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
                    .then(Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self))
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistformStalker))
        return driver
    }

    test("Mistform Stalker is a 1/1 Illusion") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stalker = driver.putCreatureOnBattlefield(activePlayer, "Mistform Stalker")
        val projected = projector.project(driver.state)

        projected.getPower(stalker) shouldBe 1
        projected.getToughness(stalker) shouldBe 1
        projected.hasKeyword(stalker, Keyword.FLYING) shouldBe false
    }

    test("change type ability presents creature type choice") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stalker = driver.putCreatureOnBattlefield(activePlayer, "Mistform Stalker")
        driver.removeSummoningSickness(stalker)
        driver.giveMana(activePlayer, Color.BLUE, 1)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = stalker,
                abilityId = changeTypeAbilityId
            )
        )

        driver.bothPass()

        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        // Choose "Elf"
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        driver.isPaused shouldBe false
    }

    test("pump ability grants +2/+2 and flying until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stalker = driver.putCreatureOnBattlefield(activePlayer, "Mistform Stalker")
        driver.removeSummoningSickness(stalker)
        driver.giveMana(activePlayer, Color.BLUE, 4)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = stalker,
                abilityId = pumpAbilityId
            )
        )

        driver.bothPass()

        // After resolving, stalker should be 3/3 with flying
        val projected = projector.project(driver.state)
        projected.getPower(stalker) shouldBe 3
        projected.getToughness(stalker) shouldBe 3
        projected.hasKeyword(stalker, Keyword.FLYING) shouldBe true
    }
})
