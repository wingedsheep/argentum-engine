package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chulane, Teller of Tales — Throne of Eldraine #326 (canonical printing)
 * {2}{G}{W}{U} · Legendary Creature — Human Druid · 2/4
 *
 * Vigilance
 * Whenever you cast a creature spell, draw a card, then you may put a land card from your hand
 * onto the battlefield.
 * {3}, {T}: Return target creature you control to its owner's hand.
 *
 * The land drop is [Patterns.Hand.putFromHand]'s up-to-one selection (the "you may", Burgeoning's
 * shape); it is a put, not a land play, so it ignores the land-per-turn limit.
 */
val ChulaneTellerOfTales = card("Chulane, Teller of Tales") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Druid"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n" +
        "Whenever you cast a creature spell, draw a card, then you may put a land card from your " +
        "hand onto the battlefield.\n" +
        "{3}, {T}: Return target creature you control to its owner's hand."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.DrawCards(1) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ReturnToHand(creature)
        description = "Return target creature you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "326"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83f43730-1c1f-4150-8771-d901c54bedc4.jpg?1783932548"
        ruling(
            "2019-10-04",
            "Chulane's triggered ability resolves before the spell that caused it to trigger. It " +
                "resolves even if that spell is countered."
        )
        ruling(
            "2019-10-04",
            "Chulane's triggered ability doesn't count as playing a land. It can put a land card onto " +
                "the battlefield even if you've already played your land for the turn, and even if " +
                "it's not your turn."
        )
    }
}
