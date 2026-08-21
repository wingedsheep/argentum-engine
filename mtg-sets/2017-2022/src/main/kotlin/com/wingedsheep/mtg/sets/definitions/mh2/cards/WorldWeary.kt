package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * World-Weary — Modern Horizons 2 #109
 * {3}{B}{B} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -4/-4.
 * Basic landcycling {1}{B} ({1}{B}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 *
 * `auraTarget` carries the "Enchant creature" line — it is both the cast-time target and the
 * standing attachment restriction (CR 303.4). The stat modification is a *filterless* [ModifyStats]
 * static ability: with no filter it applies to whatever the Aura is attached to, so it needs no
 * `Filters.EnchantedCreature` spelling and follows the attachment automatically. -4/-4 is a layer
 * 7c modification, not damage, so the creature dies to state-based actions rather than being
 * destroyed by the Aura.
 *
 * Basic landcycling ([KeywordAbility.basicLandcycling]) narrows the shared typecycling search to
 * *basic* land cards.
 */
val WorldWeary = card("World-Weary") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -4/-4.\n" +
        "Basic landcycling {1}{B} ({1}{B}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(-4, -4)
    }

    keywordAbility(KeywordAbility.basicLandcycling("{1}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c7c1b08-3184-478f-92be-88db6cda33c5.jpg?1783926852"
    }
}
