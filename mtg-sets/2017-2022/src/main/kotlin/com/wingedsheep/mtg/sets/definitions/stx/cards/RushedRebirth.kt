package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rushed Rebirth — Strixhaven: School of Mages #228 (canonical printing)
 * {B}{G} · Instant
 *
 * Choose target creature. When that creature dies this turn, search your library for a creature
 * card with lesser mana value, put it onto the battlefield tapped, then shuffle.
 *
 * Reincarnation's watched-creature delayed trigger (`Triggers.self.dies()` on the target, expiring
 * at end of turn) paying off with Eldritch Evolution's capped library search. "Lesser mana value"
 * is the strict `<` of Riveteers Ascendancy — `manaValueAtMostDynamic(triggeringManaValue() - 1)`,
 * read off the dead creature's last-known mana value as the trigger resolves (an {X} in either
 * mana cost counts as 0, ruling). The search is always made by Rushed Rebirth's controller, in
 * their library, whoever controlled the creature.
 */
val RushedRebirth = card("Rushed Rebirth") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Choose target creature. When that creature dies this turn, search your library for " +
        "a creature card with lesser mana value, put it onto the battlefield tapped, then shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.CreateDelayedTrigger(
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.manaValueAtMostDynamic(
                    DynamicAmounts.triggeringManaValue() - 1
                ),
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true,
            ),
            trigger = Triggers.self.dies(),
            watchedTarget = creature,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Jason Kang"
        flavorText = "Witherbloom students learn to see death not as tragedy, but as opportunity."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07b7c952-5ab7-4546-8369-4d213bf868bf.jpg?1783927295"
        ruling("2021-04-16", "If you choose a creature or search for a creature card with {X} in its mana cost, X is considered to be 0.")
    }
}
