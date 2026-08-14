package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wreck Remover — Aetherdrift #247
 * {4} · Artifact Creature — Construct · 3/4
 *
 * Whenever this creature enters or attacks, exile up to one target card from a graveyard.
 * You gain 1 life.
 * Cycling {2}
 *
 * "Enters or attacks" is one printed ability with two trigger conditions; the SDK has no combined
 * trigger spec, so it is expressed as two `triggeredAbility` blocks with identical effects — the
 * established shape here (Sentinel of the Nameless City). Only one of the two can ever fire from
 * a single event, so the split never double-triggers.
 *
 * The graveyard target is **up to one** (`optional = true`), and it may sit in *any* graveyard
 * ([TargetFilter.CardInGraveyard] is not owner-scoped). The life gain is not conditional on the
 * exile: with no target chosen, or with the chosen card gone by resolution, the controller still
 * gains 1 — so the gain is a sibling in the [Effects.Composite], not a rider on the move.
 */
val WreckRemover = card("Wreck Remover") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature enters or attacks, exile up to one target card from a " +
        "graveyard. You gain 1 life.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "up to one target card in a graveyard",
            TargetObject(optional = true, filter = TargetFilter.CardInGraveyard)
        )
        effect = Effects.Composite(
            Effects.Move(t, Zone.EXILE),
            Effects.GainLife(1)
        )
        description = "Whenever this creature enters or attacks, exile up to one target card from " +
            "a graveyard. You gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target(
            "up to one target card in a graveyard",
            TargetObject(optional = true, filter = TargetFilter.CardInGraveyard)
        )
        effect = Effects.Composite(
            Effects.Move(t, Zone.EXILE),
            Effects.GainLife(1)
        )
        description = "Whenever this creature enters or attacks, exile up to one target card from " +
            "a graveyard. You gain 1 life."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Villarrte"
        flavorText = "\"Don't get too cocky. We're all junk to the cleanup crew.\"\n—Far Fortune"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3151960-cc0c-47b5-b476-295d7a17ae14.jpg?1783907845"
    }
}
