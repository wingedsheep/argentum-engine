package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek's Sentinel — Battle for Zendikar #129
 * {1}{R} · Creature — Eldrazi Drone · 1/4
 * Devoid (This card has no color.)
 * Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn.
 *
 * The cast trigger filters the spell on `CardPredicate.IsColorless` — Sky Scourer's shape — so a
 * devoid spell or an artifact counts.
 */
val KozileksSentinel = card("Kozilek's Sentinel") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless))
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Raymond Swanland"
        flavorText = "It moves through the tunnels as slowly and surely as the shifting stones."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/786c6a0f-3f75-45ff-aae9-5c866be279d0.jpg?1783938198"
    }
}
