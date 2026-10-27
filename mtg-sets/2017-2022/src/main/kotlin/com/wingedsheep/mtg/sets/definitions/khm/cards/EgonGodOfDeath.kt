package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Egon, God of Death // Throne of Death — Kaldheim #92 (canonical printing)
 * Legendary Creature — God 6/6 // Legendary Artifact · Modal double-faced card
 *
 * Front: the upkeep trigger is an explicit [Effects.If] on "two or more cards in your graveyard",
 * checked as it resolves. Per the rulings, with only one card in the graveyard nothing is exiled
 * (a bare choose-exactly-two would wrongly exile the lone card), and with two or more the exile is
 * mandatory — the player can't opt into the sacrifice-and-draw branch. The failing branch
 * sacrifices Egon (a no-op if it has already left the battlefield) and still draws a card.
 *
 * Back: the mill trigger and the draw ability with its graveyard-exile cost, paid before anyone
 * can respond.
 */
private val EgonGodOfDeathFront = card("Egon, God of Death") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — God"
    power = 6
    toughness = 6
    oracleText = "Deathtouch\n" +
        "At the beginning of your upkeep, exile two cards from your graveyard. If you can't, sacrifice Egon and draw a card."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.CardsInGraveyardAtLeast(2),
            then = Effects.Pipeline {
                val graveyardCards = gather(CardSource.FromZone(zone = Zone.GRAVEYARD))
                val toExile = chooseExactly(2, from = graveyardCards, selectedLabel = "Exile", name = "toExile")
                exile(toExile)
            },
            otherwise = Effects.SacrificeTarget(EffectTarget.Self) then Effects.DrawCards(1),
        )
        description = "At the beginning of your upkeep, exile two cards from your graveyard. " +
            "If you can't, sacrifice Egon and draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Jason A. Engle"
        flavorText = "Egon ages backward, his wisdom soured by youthful spite."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dfdb73d-b001-4a59-b79e-8c8c1baea116.jpg?1783928254"
        ruling("2021-02-05", "You choose which cards you're exiling as Egon's triggered ability is resolving. If there are two or more cards in your graveyard at that time, you must exile two of them. You can't choose not to exile cards and sacrifice Egon instead to draw a card.")
        ruling("2021-02-05", "If there's only one card in your graveyard, you won't exile it. Because you can't exile two cards from your graveyard, you'll sacrifice Egon and draw a card.")
        ruling("2021-02-05", "If Egon leaves the battlefield before its triggered ability resolves, you'll still exile two cards from your graveyard if you can and draw a card if you can't.")
    }
}

private val ThroneOfDeathBack = card("Throne of Death") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your upkeep, mill a card.\n" +
        "{2}{B}, {T}, Exile a creature card from your graveyard: Draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Library.mill(1)
        description = "At the beginning of your upkeep, mill a card."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Tap,
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature),
        )
        effect = Effects.DrawCards(1)
        description = "{2}{B}, {T}, Exile a creature card from your graveyard: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Jason A. Engle"
        flavorText = "It offers all the warmth and comfort of the grave."
        imageUri = "https://cards.scryfall.io/normal/back/9/d/9dfdb73d-b001-4a59-b79e-8c8c1baea116.jpg?1783928254"
        ruling("2021-02-05", "Although players may respond to Throne of Death's activated ability, they can't respond to the paying of its activation cost. The creature card will already be exiled by the time anyone can respond.")
    }
}

val EgonGodOfDeath: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = EgonGodOfDeathFront,
    backFace = ThroneOfDeathBack,
)
