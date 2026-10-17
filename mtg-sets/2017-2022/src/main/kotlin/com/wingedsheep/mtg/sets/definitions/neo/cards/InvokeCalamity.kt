package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invoke Calamity — Kamigawa: Neon Dynasty #147
 * {1}{R}{R}{R}{R} · Instant
 *
 * You may cast up to two instant and/or sorcery spells with total mana value 6 or less from your
 * graveyard and/or hand without paying their mana costs. If those spells would be put into your
 * graveyard, exile them instead. Exile Invoke Calamity.
 *
 * The instants and sorceries in your hand and graveyard are gathered, then offered one at a time
 * during resolution by the capped free-cast loop: at most two casts, each spending its mana value
 * from a budget of 6 (X is 0 on a free cast), with each spell exiled instead of going to your
 * graveyard. Invoke Calamity itself is on the stack while it resolves, so it can't be one of them.
 */
val InvokeCalamity = card("Invoke Calamity") {
    manaCost = "{1}{R}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may cast up to two instant and/or sorcery spells with total mana value 6 or less " +
        "from your graveyard and/or hand without paying their mana costs. If those spells would be put " +
        "into your graveyard, exile them instead. Exile Invoke Calamity."

    spell {
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND),
                    player = Player.You,
                    filter = GameObjectFilter.InstantOrSorcery,
                )
            )
            run(
                Effects.CastUpToNFromCollectionWithoutPayingCost(
                    candidates,
                    maxCasts = 2,
                    maxTotalManaValue = 6,
                    insteadOfGraveyard = AfterResolveDestination.EXILE,
                )
            )
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe666c86-6734-4c47-9244-bcd26b54068d.jpg?1783923866"
    }
}
