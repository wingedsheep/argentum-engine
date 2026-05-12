package com.wingedsheep.engine.handlers.keywords

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainCastOf
import com.wingedsheep.engine.legalactions.support.shouldNotContainCastOf
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val MayhemTestCreature = CardDefinition(
    name = "Mayhem Test Creature",
    manaCost = ManaCost.parse("{3}{R}{R}"),
    typeLine = TypeLine.creature(setOf(Subtype("Goblin"))),
    oracleText = "Mayhem {2}{R} (You may cast this card from your graveyard for its mayhem cost if you discarded it this turn. Then exile it.)",
    creatureStats = CreatureStats(3, 3),
    keywordAbilities = listOf(KeywordAbility.Mayhem(ManaCost.parse("{2}{R}")))
)

class MayhemTest : FunSpec({

    context("Mayhem (graveyard alternative cost — discarded this turn only)") {

        test("Mayhem card discarded this turn is offered as CastWithMayhem from graveyard") {
            val driver = setupP1(
                battlefield = listOf("Mountain", "Mountain", "Mountain"),
                graveyard = listOf("Mayhem Test Creature"),
                extraSetCards = listOf(MayhemTestCreature)
            )

            val graveyardKey = ZoneKey(driver.player1, Zone.GRAVEYARD)
            val cardId = driver.game.state.getZone(graveyardKey).first { id ->
                driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Mayhem Test Creature"
            }
            val playerEntity = driver.game.state.getEntity(driver.player1)!!
                .with(CardsDiscardedThisTurnComponent(setOf(cardId)))
            driver.game.replaceState(driver.game.state.withEntity(driver.player1, playerEntity))

            val view = driver.enumerateFor(driver.player1)

            view shouldContainCastOf "Mayhem Test Creature"
            val mayhemCast = view.castActionsFor("Mayhem Test Creature").first()
            mayhemCast.actionType shouldBe "CastWithMayhem"
            mayhemCast.affordable shouldBe true
            mayhemCast.manaCostString shouldBe "{2}{R}"
            mayhemCast.sourceZone shouldBe "GRAVEYARD"
            (mayhemCast.action as CastSpell).useAlternativeCost shouldBe true
        }

        test("Mayhem card NOT discarded this turn is NOT offered the Mayhem alternative cost") {
            val driver = setupP1(
                battlefield = listOf("Mountain", "Mountain", "Mountain"),
                graveyard = listOf("Mayhem Test Creature"),
                extraSetCards = listOf(MayhemTestCreature)
            )
            // No CardsDiscardedThisTurnComponent — card reached the graveyard by other means.

            driver.enumerateFor(driver.player1) shouldNotContainCastOf "Mayhem Test Creature"
        }
    }
})
