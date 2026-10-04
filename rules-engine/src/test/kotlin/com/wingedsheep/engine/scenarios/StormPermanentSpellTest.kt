package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Storm on a permanent spell (CR 702.40a): the spell has no spell effect, so the copies are put on
 * the stack as permanent spells and each becomes a token as it resolves (CR 707.10f).
 */
class StormPermanentSpellTest : FunSpec({

    val stormBear = CardDefinition.creature(
        name = "Storm Bear",
        manaCost = ManaCost.parse("{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.STORM)
    )

    test("a creature spell with storm copies itself and the copies resolve as tokens") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(stormBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 2))
        driver.putLandOnBattlefield(caster, "Forest")
        val bear = driver.putCardInHand(caster, "Storm Bear")

        driver.castSpell(caster, bear).outcome shouldBe Outcome.Done

        val storm = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.effect as? StormCopyEffect
        }.single()
        storm.copyCount shouldBe 2
        storm.spellEffect shouldBe null

        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 10) {
            driver.bothPass()
            guard++
        }

        val bears = driver.state.getBattlefield(caster).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Storm Bear"
        }
        bears.size shouldBe 3
        bears.count { driver.state.getEntity(it)!!.has<TokenComponent>() } shouldBe 2
        driver.state.getEntity(bear)!!.has<TokenComponent>() shouldBe false
    }
})
