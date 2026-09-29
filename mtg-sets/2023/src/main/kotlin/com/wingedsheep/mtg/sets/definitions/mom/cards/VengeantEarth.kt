package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vengeant Earth
 * {1}{G}
 * Instant
 * Target creature or land you control becomes a 4/4 Elemental creature with haste in addition to
 * its other types until end of turn. It must be blocked this turn if able.
 *
 * "In addition to its other types" keeps a targeted creature's existing creature types (CR 205.1b),
 * so Elemental is *added* rather than set through `BecomeCreature(creatureTypes = ...)`, which would
 * replace them.
 */
val VengeantEarth = card("Vengeant Earth") {
    manaCost = "{1}{G}"
    typeLine = "Instant"
    oracleText = "Target creature or land you control becomes a 4/4 Elemental creature with haste in " +
        "addition to its other types until end of turn. It must be blocked this turn if able."

    spell {
        val t = target(TargetFilter.CreatureOrLandPermanent.youControl())
        effect = Effects.BecomeCreature(
            target = t,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.HASTE),
            duration = Duration.EndOfTurn
        ) then
            Effects.AddSubtype("Elemental", t, Duration.EndOfTurn) then
            Effects.MustBeBlocked(t, allCreatures = false)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "212"
        artist = "Jonas De Ro"
        flavorText = "When Zendikar's defenders faltered, the Roil rose up to shake the invaders from its back."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16e20076-5d24-4c59-b707-6d20e121032f.jpg?1783916959"
        ruling("2023-04-14", "If Vengeant Earth targets a creature you control, Vengeant Earth will overwrite any previous effects that set the creature's power and toughness to specific values. Effects that otherwise modify the target creature's power and toughness will still apply no matter when they took effect. The same is true for +1/+1 counters.")
        ruling("2023-04-14", "The target creature or land will keep any abilities it previously had.")
        ruling("2023-04-14", "You may target a land that's already a creature. For example, if you target a land that's also a 0/0 creature and has three +1/+1 counters on it, the resulting land creature will be 7/7.")
    }
}
