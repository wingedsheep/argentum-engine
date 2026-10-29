package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chandra, Heart of Fire
 * {3}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty: 5
 *
 * +1: Discard your hand, then exile the top three cards of your library. Until end of turn, you
 *     may play cards exiled this way.
 * +1: Chandra deals 2 damage to any target.
 * −9: Search your graveyard and library for any number of red instant and/or sorcery cards,
 *     exile them, then shuffle. You may cast them this turn. Add six {R}.
 *
 * The first +1 is a hand discard followed by a three-card impulse; the three cards are exiled
 * even with an empty hand (ruling). The −9 is one search across graveyard and library (a gather
 * flagged as a library search), an any-number pick, exile, shuffle, then an end-of-turn
 * play-from-exile grant over exactly the cards exiled — they're instants and sorceries, so
 * "play" is "cast" — and the {R}{R}{R}{R}{R}{R}. Normal timing still applies to those casts.
 */
val ChandraHeartOfFire = card("Chandra, Heart of Fire") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 5
    oracleText = "+1: Discard your hand, then exile the top three cards of your library. Until end of turn, " +
        "you may play cards exiled this way.\n" +
        "+1: Chandra deals 2 damage to any target.\n" +
        "−9: Search your graveyard and library for any number of red instant and/or sorcery cards, " +
        "exile them, then shuffle. You may cast them this turn. Add six {R}."

    loyaltyAbility(+1) {
        effect = Patterns.Hand.discardHand() then Patterns.Exile.impulse(3)
        description = "Discard your hand, then exile the top three cards of your library. Until end of turn, " +
            "you may play cards exiled this way."
    }

    loyaltyAbility(+1) {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(2, anyTarget)
        description = "Chandra deals 2 damage to any target."
    }

    loyaltyAbility(-9) {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.LIBRARY),
                    player = Player.You,
                    filter = GameObjectFilter.InstantOrSorcery.withColor(Color.RED)
                ),
                search = true
            )
            val found = chooseAnyNumber(
                searchable,
                prompt = "Search your graveyard and library for any number of red instant and/or sorcery cards to exile"
            )
            exile(found)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the library was searched whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
            run(Effects.GrantMayPlayFromExile(found))
            run(Effects.AddMana(Color.RED, 6))
        }
        description = "Search your graveyard and library for any number of red instant and/or sorcery cards, " +
            "exile them, then shuffle. You may cast them this turn. Add six {R}."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "135"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c3ca8c-c77c-43b8-84ad-796313ecc813.jpg?1783930694"
        ruling("2020-06-23", "If you have no cards in hand, you still exile three cards while resolving Chandra's first ability and you may play them this turn.")
        ruling("2020-06-23", "You must follow the normal timing permissions and restrictions for the exiled cards for Chandra's first and last abilities. If one is a land, you can't play it unless you have land plays available.")
        ruling("2020-06-23", "Because it's a loyalty ability, Chandra's last ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
    }
}
