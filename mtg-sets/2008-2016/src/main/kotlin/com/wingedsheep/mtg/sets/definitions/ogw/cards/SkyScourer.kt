package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sky Scourer — Oath of the Gatewatch #78
 * {1}{B} · Creature — Eldrazi Drone · Common
 * 1/1
 *
 * Devoid (This card has no color.)
 * Flying
 * Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn.
 *
 * Implementation notes:
 * - The cast trigger filters the spell on `CardPredicate.IsColorless` (Thought Harvester's shape),
 *   so a devoid spell or an artifact counts.
 */
val SkyScourer = card("Sky Scourer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless))
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Whenever you cast a colorless spell, this creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Winona Nelson"
        flavorText = "\"The Eldrazi are nothing if not thorough.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb00fa72-bcda-4081-9048-e13752da8d46.jpg?1783937913"
    }
}
