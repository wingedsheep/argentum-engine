package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chandra's Flame Wave — Core Set 2020 #295 (planeswalker deck)
 * {3}{R}{R} · Sorcery
 *
 * Chandra's Flame Wave deals 2 damage to target player and each creature that player controls.
 * Search your library and/or graveyard for a card named Chandra, Flame's Fury, reveal it, and put
 * it into your hand. If you search your library this way, shuffle.
 *
 * The damage half is Chandra, Flame's Fury's −8 shape (only the player is targeted); the search is
 * mandatory here, unlike the "you may" planeswalker-deck tutors.
 */
val ChandrasFlameWave = card("Chandra's Flame Wave") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Chandra's Flame Wave deals 2 damage to target player and each creature that player " +
        "controls. Search your library and/or graveyard for a card named Chandra, Flame's Fury, reveal " +
        "it, and put it into your hand. If you search your library this way, shuffle."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DealDamage(2, player) then
            Patterns.Group.dealDamageToAll(2, GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Chandra, Flame's Fury"),
                destination = SearchDestination.HAND,
                reveal = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "295"
        artist = "Paul Scott Canavan"
        flavorText = "\"It's high tide.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f13b6a7-fa62-4d94-a56c-f2e64c8c1666.jpg?1783932918"
        ruling("2019-07-12", "If the target player is an illegal target by the time Chandra's Flame Wave resolves, the spell doesn't resolve. No creatures or players are dealt damage, and you won't search for Chandra, Flame's Fury.")
        ruling("2019-07-12", "Chandra's Flame Wave targets the player, not their creatures. A creature with hexproof may be dealt damage this way.")
    }
}
