package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Wondrous Wasp — Marvel Super Heroes #84 (rare)
 * {1}{U} · Legendary Creature — Human Hero · 2/1
 *
 * Flash
 * Flying
 * Wasp's Sting — When The Wondrous Wasp enters, tap up to one target creature. It loses all
 * abilities for as long as The Wondrous Wasp remains on the battlefield.
 *
 * Implementation notes:
 * - Flash plus the ETB strip is the Merfolk Trickster / Tishana's Tidebinder combat trick: hold it
 *   up, flash it in, and shut a creature's abilities off — except that here the shutdown is not
 *   "until end of turn" but the *source-keyed* [Duration.WhileSourceOnBattlefield] (CR 611.2b).
 *   That duration is one-way: the moment the Wasp leaves the battlefield the floating Layer-6
 *   "loses all abilities" effect is physically removed (`EndedDurationExpiryCheck`), and a later
 *   Wasp entering does not resume it.
 * - "up to one target creature" is an *optional* [TargetCreature] — declining the target makes both
 *   halves of the ability a no-op, and if the creature becomes an illegal target before resolution
 *   the whole trigger is countered for having no legal targets. Neither half is a separate target,
 *   so "it" in the second sentence is the same bound creature.
 * - The tap resolves first, then the strip. Order matters only cosmetically here (the strip does
 *   not remove the creature's tapped status), but it is the printed order.
 * - "Wasp's Sting" is an ability word: flavor only, carried in the oracle text and the ability
 *   description, with no rules meaning.
 */
val TheWondrousWasp = card("The Wondrous Wasp") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Hero"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Flying\n" +
        "Wasp's Sting — When The Wondrous Wasp enters, tap up to one target creature. It loses " +
        "all abilities for as long as The Wondrous Wasp remains on the battlefield."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "up to one target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature)
        )
        effect = Effects.Composite(
            Effects.Tap(victim),
            Effects.RemoveAllAbilities(
                victim,
                Duration.WhileSourceOnBattlefield("The Wondrous Wasp")
            ),
        )
        description = "Wasp's Sting — When The Wondrous Wasp enters, tap up to one target " +
            "creature. It loses all abilities for as long as The Wondrous Wasp remains on the " +
            "battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Gal Or"
        flavorText = "\"Don't overlook the little things.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/484e80a4-1d5f-4c27-98a8-ee2ef9d6cbdc.jpg?1783902947"
    }
}
