package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Soul Foundry
 * {4}
 * Artifact
 *
 * Imprint — When this artifact enters, you may exile a creature card from your hand.
 * {X}, {T}: Create a token that's a copy of the exiled card. X is the mana value of that card.
 *
 * The imprint half is the Isochron Scepter shape: a linked exile (`linkToSource = true`) from your
 * own hand with no reveal, so the pile the activated ability reads is re-read live — if the
 * imprinted card leaves exile the gather comes back empty and no token is created.
 *
 * The activation cost is the interesting half. It is printed as `{X}` but the player never picks a
 * number: `xDefinedAs` hands the engine the mana value of the linked exiled card, which is CR
 * 107.3c's "the value of X is defined by the text of that ability". The `{X}` is substituted before
 * affordability, so the ability is offered at its resolved price ("{3}, {T}" for an imprinted
 * three-drop) and there is no X prompt. With no imprint the amount is 0 and the ability is a legal
 * — and entirely pointless — "{0}, {T}", exactly as printed.
 *
 * The token is a copy of the *card in exile*, so per the card's own rulings it keeps that card's
 * mana cost and mana value rather than a token's usual zero, and it is put onto the battlefield
 * rather than cast — which is why no optional additional cost (kicker) is ever offered.
 */
val SoulFoundry = card("Soul Foundry") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Imprint — When this artifact enters, you may exile a creature card from your hand.\n" +
        "{X}, {T}: Create a token that's a copy of the exiled card. X is the mana value of that card."

    // Imprint — When this artifact enters, you may exile a creature card from your hand.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Patterns.Hand.revealHandAndExileChosen(
                target = EffectTarget.Controller,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card to exile",
                storeChosenAs = "foundryImprint",
                revealHand = false,
                linkToSource = true
            ),
            descriptionOverride = "You may exile a creature card from your hand."
        )
    }

    // {X}, {T}: Create a token that's a copy of the exiled card. X is the mana value of that card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        xDefinedAs = DynamicAmount.EntityProperty(
            EntityReference.LinkedExiledCard(),
            EntityNumericProperty.ManaValue
        )
        // No `description` override on the ability itself: the generated label carries the
        // *resolved* cost ("{3}, {T}: …" for an imprinted three-drop), which is the whole point of
        // a defined X, and an ability-level override would freeze the printed "{X}" instead.
        effect = Effects.Composite(
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromLinkedExile(),
                    storeAs = "foundryImprinted"
                ),
                Effects.CreateTokenCopyOfTarget(
                    target = EffectTarget.PipelineTarget("foundryImprinted")
                )
            ),
            descriptionOverride = "Create a token that's a copy of the exiled card."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11b4983a-2a95-4322-a315-27b4bd430d39.jpg?1783944502"

        ruling(
            "2004-12-01",
            "Soul Foundry puts a token copy of the imprinted card onto the battlefield. The token " +
                "is put onto the battlefield, not cast."
        )
        ruling(
            "2004-12-01",
            "The token is an exact copy in every way, except that it's a token, not a card."
        )
        ruling(
            "2004-12-01",
            "Most creature tokens have no mana cost and a mana value of 0, but a creature token " +
                "put onto the battlefield by Soul Foundry has the same mana cost and mana value as " +
                "the card it copies."
        )
        ruling(
            "2004-10-04",
            "You do not get a chance to pay any optional costs, such as Kicker, on the imprinted card."
        )
    }
}
