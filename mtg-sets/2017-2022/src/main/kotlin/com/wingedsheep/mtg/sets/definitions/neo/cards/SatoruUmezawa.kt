package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Satoru Umezawa — Kamigawa: Neon Dynasty #234 (canonical printing)
 * {1}{U}{B} · Legendary Creature — Human Ninja · 2/4
 *
 * Whenever you activate a ninjutsu ability, look at the top three cards of your library. Put one
 * of them into your hand and the rest on the bottom of your library in any order. This ability
 * triggers only once each turn.
 * Each creature card in your hand has ninjutsu {2}{U}{B}.
 *
 * Ninjutsu is a hand-activated ability tagged `keyword = NINJUTSU` (CR 702.49a), so the trigger
 * keys off that marker and counts a printed or a granted ninjutsu alike. The grant hands every
 * creature card in your hand the same [ninjutsuAbility] a printed `ninjutsu("{2}{U}{B}")` builds;
 * a card with its own ninjutsu at a different cost keeps both and you pick which to activate (at
 * the same cost the two are one ability — the id is keyword-and-cost scoped).
 */
val SatoruUmezawa = card("Satoru Umezawa") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Ninja"
    power = 2
    toughness = 4
    oracleText = "Whenever you activate a ninjutsu ability, look at the top three cards of your " +
        "library. Put one of them into your hand and the rest on the bottom of your library in any " +
        "order. This ability triggers only once each turn.\n" +
        "Each creature card in your hand has ninjutsu {2}{U}{B}."

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(keyword = Keyword.NINJUTSU)
        oncePerTurn = true
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ninjutsuAbility("{2}{U}{B}"),
            GroupFilter(GameObjectFilter.Creature.ownedByYou()),
            recipientZone = Zone.HAND,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8887f26d-b097-4fbc-9c48-bdc656409a32.jpg?1783923830"
    }
}
