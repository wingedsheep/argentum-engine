package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Autumnal Gloom // Ancient of the Equinox (Shadows over Innistrad #194)
 *
 * Front — {2}{G} Enchantment
 * {B}: Mill a card.
 * Delirium — At the beginning of your end step, if there are four or more card types among cards in
 * your graveyard, transform this enchantment.
 *
 * Back — Creature — Treefolk 4/4 (green color indicator)
 * Trample, hexproof
 *
 * The delirium trigger is an intervening "if": it doesn't trigger without four card types, and is
 * re-checked on resolution.
 */
private val AutumnalGloomFront = card("Autumnal Gloom") {
    manaCost = "{2}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "{B}: Mill a card.\n" +
        "Delirium — At the beginning of your end step, if there are four or more card types among " +
        "cards in your graveyard, transform this enchantment."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Patterns.Library.mill(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.Delirium(4)
        effect = Effects.Transform(EffectTarget.Self)
        description = "Delirium — At the beginning of your end step, if there are four or more card " +
            "types among cards in your graveyard, transform this enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d41b07c8-f0c8-4654-bcc2-00c785c8c628.jpg?1783937742"
        ruling("2016-04-08", "Because you consider only the characteristics of a double-faced card's front face while it's not on the battlefield, the types of its back face won't be counted for delirium.")
        ruling("2016-04-08", "Most triggered delirium abilities use an intervening \"if\" clause. There must be four or more card types among cards in your graveyard in order for these abilities to trigger, otherwise they never trigger at all. The number of card types is checked again as the trigger resolves, and if it has become too low somehow, the ability does nothing.")
    }
}

private val AncientOfTheEquinox = card("Ancient of the Equinox") {
    manaCost = ""
    colorIdentity = "BG"
    colorIndicator = "G"
    typeLine = "Creature — Treefolk"
    power = 4
    toughness = 4
    oracleText = "Trample, hexproof"

    keywords(Keyword.TRAMPLE, Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Jaime Jones"
        flavorText = "\"When the leaves fall and the nights grow cold, look elsewhere for firewood.\"\n—Ola of Lambholt"
        imageUri = "https://cards.scryfall.io/normal/back/d/4/d41b07c8-f0c8-4654-bcc2-00c785c8c628.jpg?1783937742"
    }
}

val AutumnalGloom: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AutumnalGloomFront,
    backFace = AncientOfTheEquinox,
)
