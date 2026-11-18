package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Worldwaker — Magic 2015 #187 (canonical printing; the earlier PS14 copy is a promo)
 * {3}{G}{G} · Legendary Planeswalker — Nissa · Loyalty 3
 *
 * +1: Target land you control becomes a 4/4 Elemental creature with trample. It's still a land.
 * +1: Untap up to four target Forests.
 * −7: Search your library for any number of basic land cards, put them onto the battlefield,
 *     then shuffle. Those lands become 4/4 Elemental creatures with trample. They're still lands.
 *
 * - Neither animation has a duration (ruling), so both are [Duration.Permanent].
 *   [Effects.BecomeCreature] adds the creature type and the Elemental subtype without removing
 *   Land or the land's own subtypes and abilities, so the lands are still lands (Elemental
 *   Uprising's shape, minus the end-of-turn duration).
 * - "Forests" is the Forest land subtype, under any player's control (Nissa, Sage Animist's
 *   untap-each-target shape).
 * - The −7 is Rampaging Growth's search with `chooseAnyNumber`: `moveTracked` records exactly
 *   the lands that entered, and only those are animated, after the shuffle. They enter first
 *   and only then become creatures, so "creature enters" triggers don't see them.
 */
val NissaWorldwaker = card("Nissa, Worldwaker") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 3
    oracleText = "+1: Target land you control becomes a 4/4 Elemental creature with trample. It's still a land.\n" +
        "+1: Untap up to four target Forests.\n" +
        "−7: Search your library for any number of basic land cards, put them onto the battlefield, " +
        "then shuffle. Those lands become 4/4 Elemental creatures with trample. They're still lands."

    // +1: Target land you control becomes a 4/4 Elemental creature with trample. It's still a land.
    loyaltyAbility(+1) {
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.BecomeCreature(
            target = land,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.TRAMPLE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.Permanent,
        )
        description = "Target land you control becomes a 4/4 Elemental creature with trample. It's still a land."
    }

    // +1: Untap up to four target Forests.
    loyaltyAbility(+1) {
        targets(TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.FOREST)), count = 4, optional = true)
        effect = Effects.ForEachTarget(Effects.Untap(EffectTarget.ContextTarget(0)))
        description = "Untap up to four target Forests."
    }

    // −7: Search for any number of basic lands, put them onto the battlefield, shuffle; those
    // lands become 4/4 Elemental creatures with trample.
    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseAnyNumber(
                from = searchable,
                prompt = "Search your library for any number of basic land cards to put onto the battlefield"
            )
            val landed = moveTracked(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(
                Effects.ForEachInCollection(
                    landed,
                    Effects.BecomeCreature(
                        target = EffectTarget.IterationEntity,
                        power = 4,
                        toughness = 4,
                        keywords = setOf(Keyword.TRAMPLE),
                        creatureTypes = setOf("Elemental"),
                        duration = Duration.Permanent,
                    )
                )
            )
        }
        description = "Search your library for any number of basic land cards, put them onto the battlefield, " +
            "then shuffle. Those lands become 4/4 Elemental creatures with trample. They're still lands."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07eb2476-488b-4610-8567-f0456601f7fd.jpg?1783939164"

        ruling("2014-07-18", "The type-changing effects of the first and third abilities don't have a duration. They last indefinitely.")
        ruling("2014-07-18", "If the land targeted by the first ability entered the battlefield this turn, it won't be able to attack and its activated abilities with {T} in the cost (including its mana ability) can't be activated (unless something gives it haste). The same is true for the lands put onto the battlefield with the third ability.")
    }
}
