package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ever After — Shadows over Innistrad #109 (earliest printing)
 * {4}{B}{B}
 * Sorcery
 *
 * Return up to two target creature cards from your graveyard to the battlefield. Each of those
 * creatures is a black Zombie in addition to its other colors and types. Put Ever After on the
 * bottom of its owner's library.
 *
 * Both cards return in one simultaneous move and are already black Zombies as they enter
 * ([EntersWithCharacteristics] adds, never replaces, and lasts while they stay on the battlefield),
 * so a "whenever a Zombie enters" trigger sees each of them. Ever After itself goes to the bottom
 * of its owner's library instead of the graveyard as it resolves.
 */
val EverAfter = card("Ever After") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to the battlefield. " +
        "Each of those creatures is a black Zombie in addition to its other colors and types. " +
        "Put Ever After on the bottom of its owner's library."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)
        effect = Effects.Pipeline {
            move(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD),
                entersWith = EntersWithCharacteristics(
                    addedSubtypes = setOf(Subtype.ZOMBIE),
                    addedColors = setOf(Color.BLACK),
                ),
            )
        }
        selfPutOnBottomOfLibrary()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a530a54-2711-4858-bc88-559d90be4f05.jpg?1783937775"
        ruling("2016-04-08", "If a targeted creature card is normally colorless, it will simply become black. It won’t be both black and colorless.")
    }
}
