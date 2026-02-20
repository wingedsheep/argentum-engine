package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Mistform Shrieker.
 *
 * Mistform Shrieker: {3}{U}{U}
 * Creature — Illusion
 * 3/3
 * Flying
 * {1}: Mistform Shrieker becomes the creature type of your choice until end of turn.
 * Morph {3}{U}{U}
 */
class MistformShriekerTest : FunSpec({

    val mistformShriekerAbilityId = AbilityId(UUID.randomUUID().toString())

    val MistformShrieker = CardDefinition(
        name = "Mistform Shrieker",
        manaCost = ManaCost.parse("{3}{U}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Illusion"))),
        oracleText = "Flying\n{1}: Mistform Shrieker becomes the creature type of your choice until end of turn.\nMorph {3}{U}{U}",
        creatureStats = CreatureStats(3, 3),
        keywords = setOf(Keyword.FLYING),
        script = CardScript.permanent(
            ActivatedAbility(
                id = mistformShriekerAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{1}")),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.Self
                )
            )
        ),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{3}{U}{U}")))
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistformShrieker))
        return driver
    }

    test("Mistform Shrieker is 3/3 with flying") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shrieker = driver.putCreatureOnBattlefield(activePlayer, "Mistform Shrieker")
        val projected = projector.project(driver.state)

        projected.getPower(shrieker) shouldBe 3
        projected.getToughness(shrieker) shouldBe 3
        projected.hasKeyword(shrieker, Keyword.FLYING) shouldBe true
    }

    test("creature type changes to chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shrieker = driver.putCreatureOnBattlefield(activePlayer, "Mistform Shrieker")
        driver.removeSummoningSickness(shrieker)

        driver.giveMana(activePlayer, Color.BLUE, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = shrieker,
                abilityId = mistformShriekerAbilityId
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        val projected = projector.project(driver.state)
        projected.getSubtypes(shrieker) shouldBe setOf("Goblin")
    }
})
