package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The generated effect text players see for abilities with no hand-written description — the
 * targeting banner of a triggered ability (`effectHint`), the stack item, the action menu. It is
 * built from the effect's pipeline steps, so these pin real cards whose steps used to be joined
 * one by one into text that read as implementation and got the chooser wrong ("Look at cards in
 * that player's hand. Choose exactly 2 of those cards. Put those cards into a graveyard" for a
 * *target player discards two cards*).
 *
 * Not a card test: the cards are fixtures for the step-text generator (`describeSteps`).
 */
class GeneratedEffectTextTest : FunSpec({

    val cards: Map<String, CardDefinition> by lazy {
        MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name }
    }

    fun card(name: String) = cards[name] ?: error("$name is not registered")

    test("Strands of Undeath: the target player discards, and is the one named as choosing") {
        card("Strands of Undeath").triggeredAbilities.single().effect.description shouldBe
            "Target player discards two cards"
    }

    test("Tormod's Crypt: a whole graveyard exiled names whose graveyard it is") {
        card("Tormod's Crypt").activatedAbilities.single().effect.description shouldBe
            "Exile all cards from target player's graveyard"
    }

    test("Mirkwood Nurturer: the targets returned, then an if-you-do rider") {
        card("Mirkwood Nurturer").triggeredAbilities.single().effect.description shouldBe
            "Return each target to its owner's hand. If you do, put a +1/+1 counter on this creature"
    }

    test("Thieving Sprite: the target player reveals, you choose, that player discards") {
        card("Thieving Sprite").triggeredAbilities.single().effect.description shouldBe
            "Target player reveals a number of cards from their hand equal to the number of Faerie " +
            "permanents you control. Choose one of those cards. That player discards that card"
    }

    test("Little Bear: the tested target is 'that creature', then 'it'") {
        card("Little Bear").triggeredAbilities.single().effect.description shouldBe
            "Untap another target creature you control. If that creature is a Bear creature, " +
            "put a +1/+1 counter on it"
    }
})
