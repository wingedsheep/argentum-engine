package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Return each chosen creature card that is still in your graveyard as the ability resolves. */
private val returnTargetsToHand = Effects.ForEachTarget(
    Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
)

/**
 * Soul of Innistrad — Magic 2015 #115 (canonical printing)
 * {4}{B}{B} · Creature — Avatar · 6/6
 *
 * Deathtouch
 * {3}{B}{B}: Return up to three target creature cards from your graveyard to your hand.
 * {3}{B}{B}, Exile this card from your graveyard: Return up to three target creature cards from
 * your graveyard to your hand.
 *
 * Soul of Theros's shape: both abilities share one effect; the second is activated from the
 * graveyard with exiling this card as part of its cost. The graveyard ability's targets carry no
 * "other": targets are chosen before costs are paid, so the Soul may be chosen, but it is exiled
 * by the cost and is no longer in the graveyard on resolution — an illegal target that stays in
 * exile while the others still return.
 */
val SoulOfInnistrad = card("Soul of Innistrad") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "Deathtouch\n" +
        "{3}{B}{B}: Return up to three target creature cards from your graveyard to your hand.\n" +
        "{3}{B}{B}, Exile this card from your graveyard: Return up to three target creature cards " +
        "from your graveyard to your hand."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{3}{B}{B}")
        targets(TargetFilter.CreatureInYourGraveyard, count = 3, optional = true)
        effect = returnTargetsToHand
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        targets(TargetFilter.CreatureInYourGraveyard, count = 3, optional = true)
        effect = returnTargetsToHand
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9147b57e-e172-49ac-a556-91efbde0d325.jpg?1783939180"
        ruling("2014-07-18", "You can activate the last ability only if the Soul is in your graveyard.")
        ruling("2014-07-18", "Exiling the Soul from your graveyard is part of the last ability's activation cost. A player can't remove the Soul in response to prevent you from activating the ability.")
    }
}
