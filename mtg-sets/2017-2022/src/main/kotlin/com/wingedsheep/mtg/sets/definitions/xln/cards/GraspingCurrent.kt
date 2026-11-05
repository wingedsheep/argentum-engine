package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grasping Current — Ixalan #282 (planeswalker deck)
 * {4}{U} · Sorcery
 *
 * Return up to two target creatures to their owner's hand.
 * Search your library and/or graveyard for a card named Jace, Ingenious Mind-Mage, reveal it, and
 * put it into your hand. If you search your library this way, shuffle.
 *
 * The bounce half is Into the Void's shape (one optional two-target requirement, the body run per
 * target). The search is mandatory, like Chandra's Flame Wave, and still runs when no targets were
 * chosen (ruling).
 */
val GraspingCurrent = card("Grasping Current") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creatures to their owner's hand.\nSearch your library and/or " +
        "graveyard for a card named Jace, Ingenious Mind-Mage, reveal it, and put it into your hand. " +
        "If you search your library this way, shuffle."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Jace, Ingenious Mind-Mage"),
                destination = SearchDestination.HAND,
                reveal = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "282"
        inBooster = false
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7344ffb-aada-4be1-bc82-2ed96e6cb5bf.jpg?1783935685"
        ruling("2017-09-29", "You may cast Grasping Current without choosing any target creatures. You'll just search for Jace, Ingenious Mind-Mage. However, if you choose any targets and all of those targets become illegal before Grasping Current resolves, the spell won't resolve and you won't search.")
    }
}
