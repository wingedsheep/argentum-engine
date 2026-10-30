package com.wingedsheep.engine

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * `CreateGlobalTriggeredAbility(emblem = true)` — the emblem, not the card that made it, is the
 * ability's source: a colorless object (CR 114.3) owned by the player who got it. Wrapped in
 * `ForEachPlayer(EachOpponent, …)` each opponent gets their own (Chandra, Awakened Inferno's +2,
 * covered in its scenario test). Synthetic red sorceries keep the primitive off any one card.
 */
class EmblemTriggeredAbilitySourceTest : FunSpec({

    val emblemText = "At the beginning of your upkeep, this emblem deals 1 damage to you."

    fun burnSorcery(name: String, emblem: Boolean): CardDefinition = card(name) {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "Each opponent gets an emblem with \"$emblemText\""
        spell {
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.CreateGlobalTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.you.beginningOf(Step.UPKEEP)
                        effect = Effects.DealDamage(1, EffectTarget.Controller)
                        description = emblemText
                    },
                    descriptionOverride = emblemText,
                    emblem = emblem
                )
            )
        }
    }

    val emblemBurn = burnSorcery("Test Emblem Burn", emblem = true)
    val sourcedBurn = burnSorcery("Test Sourced Burn", emblem = false)
    val redWard: CardDefinition = card("Test Red Ward") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        oracleText = "Prevent all damage that red sources would deal to you."
        replacementEffect(
            PreventDamage(
                appliesTo = EventPattern.DamageEvent(
                    recipient = Recipient.You,
                    source = GameObjectFilter.Any.withColor(Color.RED)
                )
            )
        )
    }

    fun newGame(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(emblemBurn, sourcedBurn, redWard))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String) {
        giveMana(player1, Color.RED, 1)
        castSpell(player1, putCardInHand(player1, name)).error shouldBe null
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    fun GameTestDriver.resolveOpponentUpkeep() {
        passPriorityUntil(Step.UPKEEP, player2)
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }

    test("the emblem is a colorless object controlled by the opponent who got it") {
        val d = newGame()
        d.cast("Test Emblem Burn")

        val global = d.state.globalGrantedTriggeredAbilities.single()
        global.controllerId shouldBe d.player2
        val emblem = d.state.getEntity(global.sourceId)!!
        emblem.get<EmblemSourceComponent>()!!.sourceName shouldBe "Test Emblem Burn"
        emblem.get<ControllerComponent>()!!.playerId shouldBe d.player2
        emblem.get<CardComponent>()!!.colors.shouldBeEmpty()
    }

    test("its damage is from a colorless source, so a red-damage ward doesn't stop it") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Red Ward")
        d.cast("Test Emblem Burn")
        d.resolveOpponentUpkeep()
        d.getLifeTotal(d.player2) shouldBe 19
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("without the flag the red sorcery stays the source, and the ward prevents it") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player2, "Test Red Ward")
        d.cast("Test Sourced Burn")
        d.resolveOpponentUpkeep()
        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("two emblems trigger separately") {
        val d = newGame()
        d.cast("Test Emblem Burn")
        d.cast("Test Emblem Burn")
        d.state.globalGrantedTriggeredAbilities.map { it.sourceId }.toSet().size shouldBe 2
        d.resolveOpponentUpkeep()
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("the caster's own upkeep doesn't trigger the opponent's emblem") {
        val d = newGame()
        d.cast("Test Emblem Burn")
        d.resolveOpponentUpkeep()
        d.passPriorityUntil(Step.UPKEEP, d.player1)
        var passes = 0
        while (d.stackSize > 0 && passes++ < 20) d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 19
    }
})
