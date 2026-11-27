package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Loyal Cathar // Unhallowed Cathar (Dark Ascension)
 *
 * Front — Loyal Cathar {W}{W}, Creature — Human Soldier 2/2
 *   Vigilance
 *   When this creature dies, return it to the battlefield transformed under your control at the
 *   beginning of the next end step.
 *
 * Back — Unhallowed Cathar, Creature — Zombie Soldier 2/1 (black color indicator)
 *   This creature can't block.
 *
 * The dies trigger creates a delayed trigger at the next end step (Biolume Egg's shape) whose
 * effect returns the card from the graveyard back face up under the dies trigger's controller
 * (Golden Guardian's `underYourControl`), so a stolen Cathar returns under its last controller.
 * If the card has left the graveyard by then, the return does nothing.
 */
private val LoyalCatharFront = card("Loyal Cathar") {
    manaCost = "{W}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "When this creature dies, return it to the battlefield transformed under your control at " +
        "the beginning of the next end step."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.ReturnSelfFromGraveyardTransformed(underYourControl = true),
        )
        description = "When this creature dies, return it to the battlefield transformed under " +
            "your control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Ryan Pancoast"
        flavorText = "\"I trust myself to do my duty, even unto death. It's what comes after that I'm afraid of.\"\n—Constable Visil, to Thalia"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb09041b-4d09-4cae-9e85-b859edae885b.jpg?1783940860"
        ruling("2011-01-22", "When Loyal Cathar dies, it will return to the battlefield with its back face (Unhallowed Cathar) up. It doesn't return to the battlefield and then transform.")
        ruling("2011-01-22", "If you gain control of a Loyal Cathar owned by another player and it dies, it will be put into to its owner's graveyard and then return to the battlefield under your control.")
        ruling("2016-04-08", "If a card that isn't a double-faced card becomes a copy of Loyal Cathar (due to Cytoshape, for example) and then dies, that card won't return to the battlefield. It will remain in its owner's graveyard.")
    }
}

private val UnhallowedCathar = card("Unhallowed Cathar") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "B"
    typeLine = "Creature — Zombie Soldier"
    power = 2
    toughness = 1
    oracleText = "This creature can't block."

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Ryan Pancoast"
        flavorText = "\"Just fight without fear. Your soul is protected by the hand of Avacyn and will never submit to evil.\"\n—Thalia, to Constable Visil"
        imageUri = "https://cards.scryfall.io/normal/back/c/b/cb09041b-4d09-4cae-9e85-b859edae885b.jpg?1783940860"
    }
}

val LoyalCathar: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LoyalCatharFront,
    backFace = UnhallowedCathar,
)
