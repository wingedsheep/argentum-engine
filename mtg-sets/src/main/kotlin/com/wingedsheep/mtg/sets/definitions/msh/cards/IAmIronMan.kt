package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * I Am Iron Man — Marvel Super Heroes #58
 * {2}{U} · Instant
 *
 * Until end of turn, target artifact or creature becomes an artifact creature with base power
 * and toughness 4/4 and gains flying.
 * Draw a card.
 *
 * [Effects.BecomeCreature] does the whole animate in one step: it adds the CREATURE type and the
 * ARTIFACT card type (`addTypes`, Layer 4 — both purely additive, so the target keeps its printed
 * types and subtypes), sets base power/toughness to 4/4 (Layer 7b) and grants flying (Layer 6),
 * all for [com.wingedsheep.sdk.scripting.Duration.EndOfTurn] (the default). No `creatureTypes` is
 * passed — that parameter *replaces* creature subtypes, which this card doesn't do.
 *
 * The draw is a separate sentence but part of the same resolution: if the single target is
 * illegal on resolution the spell doesn't resolve at all (CR 608.2b), so no card is drawn.
 */
val IAmIronMan = card("I Am Iron Man") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target artifact or creature becomes an artifact creature " +
        "with base power and toughness 4/4 and gains flying.\nDraw a card."

    spell {
        val t = target(
            "target artifact or creature",
            TargetPermanent(filter = TargetFilter.CreatureOrArtifact)
        )
        effect = Effects.BecomeCreature(
            target = t,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            addTypes = setOf("ARTIFACT"),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Filipe Pagliuso"
        flavorText = "\"Any questions?\"\n—Tony Stark"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c401abb-5978-41c0-962b-0432f9433929.jpg?1783902957"
    }
}
