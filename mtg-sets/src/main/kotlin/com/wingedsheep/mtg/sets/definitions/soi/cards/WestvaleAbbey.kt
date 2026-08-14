package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Westvale Abbey // Ormendahl, Profane Prince (Shadows over Innistrad #281 — the card's earliest
 * printing; also reprinted in Shadows over Innistrad Remastered and Innistrad Remastered)
 * Land // Legendary Creature — Demon 9/7
 *
 * Front — Westvale Abbey (Land)
 *   {T}: Add {C}.
 *   {5}, {T}, Pay 1 life: Create a 1/1 white and black Human Cleric creature token.
 *   {5}, {T}, Sacrifice five creatures: Transform this land, then untap it.
 *
 * Back — Ormendahl, Profane Prince (Legendary Creature — Demon, 9/7, black)
 *   Flying, lifelink, indestructible, haste
 *
 * Implementation:
 *  - A land TDFC via [CardDefinition.doubleFacedPermanent] — the same shape the Ixalan flip-lands
 *    use, just with the land on the front. The back face is a creature, so the printed keywords are
 *    plain [Keyword] grants; there is nothing to script on it.
 *  - The Cleric ability's "Pay 1 life" is a real cost atom ([Costs.PayLife]), not a resolution
 *    effect, so it composes into the activation cost alongside `{5}` and `{T}` and is checked (and
 *    paid) before the ability goes on the stack.
 *  - The flip ability's "Sacrifice five creatures" is [Costs.SacrificeMultiple]`(5, Creature)`.
 *    Westvale Abbey itself is a land, never one of the five — the filter can't match it. The
 *    sacrifices happen on activation, so the Clerics this land makes can pay for its own flip.
 *  - "Transform this land, then untap it" is one composite effect: [TransformEffect] on
 *    [EffectTarget.Self] followed by [Effects.Untap] on the same permanent. The untap matters
 *    because `{T}` is part of the cost — Ormendahl arrives untapped and, having haste, can attack
 *    the turn it flips.
 */

private val WestvaleAbbeyFront = card("Westvale Abbey") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{5}, {T}, Pay 1 life: Create a 1/1 white and black Human Cleric creature token.\n" +
        "{5}, {T}, Sacrifice five creatures: Transform this land, then untap it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.PayLife(1))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Human", "Cleric"),
            imageUri = "https://cards.scryfall.io/normal/front/9/4/94ed2eca-1579-411d-af6f-c7359c65de30.jpg?1783937680",
        )
        description = "Create a 1/1 white and black Human Cleric creature token."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}"),
            Costs.Tap,
            Costs.SacrificeMultiple(5, GameObjectFilter.Creature),
        )
        effect = Effects.Composite(
            TransformEffect(EffectTarget.Self),
            Effects.Untap(EffectTarget.Self),
        )
        description = "Transform this land, then untap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1f53d7a-9dad-46e8-b686-cd1362867445.jpg?1783937703"
        ruling(
            "2016-07-13",
            "For more information on double-faced cards, see the Shadows over Innistrad mechanics " +
                "article (http://magic.wizards.com/en/articles/archive/feature/shadows-over-innistrad-mechanics)."
        )
    }
}

private val OrmendahlProfanePrince = card("Ormendahl, Profane Prince") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Demon"
    power = 9
    toughness = 7
    oracleText = "Flying, lifelink, indestructible, haste"

    keywords(Keyword.FLYING, Keyword.LIFELINK, Keyword.INDESTRUCTIBLE, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Min Yum"
        flavorText = "With Griselbrand gone, the Skirsdag eagerly awaited another demon to claim " +
            "their devotion. Ormendahl did not make them wait long."
        imageUri = "https://cards.scryfall.io/normal/back/c/1/c1f53d7a-9dad-46e8-b686-cd1362867445.jpg?1783937703"
    }
}

val WestvaleAbbey: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = WestvaleAbbeyFront,
    backFace = OrmendahlProfanePrince,
)
