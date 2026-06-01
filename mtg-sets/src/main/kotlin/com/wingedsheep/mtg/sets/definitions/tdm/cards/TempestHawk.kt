package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Tempest Hawk — Tarkir: Dragonstorm #31
 * {2}{W} · Creature — Bird · 2/2
 *
 * Flying
 * Whenever this creature deals combat damage to a player, you may search your library
 * for a card named Tempest Hawk, reveal it, put it into your hand, then shuffle.
 * A deck can have any number of cards named Tempest Hawk.
 *
 * The final line is a deckbuilding rule (like Relentless Rats); it has no in-game effect
 * and requires no special wiring.
 */
val TempestHawk = card("Tempest Hawk") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, you may search your library " +
        "for a card named Tempest Hawk, reveal it, put it into your hand, then shuffle.\n" +
        "A deck can have any number of cards named Tempest Hawk."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.Any.named("Tempest Hawk"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
        description = "Whenever this creature deals combat damage to a player, you may search your " +
            "library for a card named Tempest Hawk, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Abz J Harding"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/422f9453-ab12-4e3c-8c51-be87391395a1.jpg?1743204081"
    }
}
