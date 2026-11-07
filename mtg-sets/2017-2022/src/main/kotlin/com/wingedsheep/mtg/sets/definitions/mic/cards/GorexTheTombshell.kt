package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

private const val GOREX_RETURN_TEXT =
    "choose a card at random exiled with Gorex and put that card into its owner's hand."

/** "Choose a card at random exiled with Gorex and put that card into its owner's hand." */
private fun returnRandomExiledCard() = Effects.Pipeline {
    val exiledWithGorex = gather(CardSource.FromLinkedExile())
    val returned = chooseRandom(1, from = exiledWithGorex)
    toHand(returned)
}

/**
 * Gorex, the Tombshell — Innistrad: Midnight Hunt Commander #20
 * {6}{B}{B} · Legendary Creature — Zombie Turtle · 4/4
 *
 * As an additional cost to cast this spell, you may exile any number of creature cards from your
 * graveyard. This spell costs {2} less to cast for each card exiled this way.
 * Deathtouch
 * Whenever Gorex attacks or dies, choose a card at random exiled with Gorex and put that card into
 * its owner's hand.
 *
 * The cost's exiled cards are linked to Gorex (CR 607), so "a card exiled with Gorex" is exactly
 * them; the dies trigger reads that pile from the departed Gorex. A graveyard holds only its owner's
 * cards, so "its owner's hand" is always yours. The discount takes generic mana only — never below
 * {B}{B} — and you may exile more cards than the discount can use (Gorex rulings).
 */
val GorexTheTombshell = card("Gorex, the Tombshell") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Turtle"
    power = 4
    toughness = 4
    oracleText = "As an additional cost to cast this spell, you may exile any number of creature cards " +
        "from your graveyard. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Deathtouch\n" +
        "Whenever Gorex attacks or dies, choose a card at random exiled with Gorex and put that card " +
        "into its owner's hand."

    additionalCost(Costs.additional.ExileAnyNumberFromGraveyardForCostReduction(GameObjectFilter.Creature, 2))

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = returnRandomExiledCard()
        description = "Whenever Gorex attacks, $GOREX_RETURN_TEXT"
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = returnRandomExiledCard()
        description = "When Gorex dies, $GOREX_RETURN_TEXT"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/398ecf98-b36b-45e1-8ac1-9d364e12d79e.jpg?1790319372"
    }
}
