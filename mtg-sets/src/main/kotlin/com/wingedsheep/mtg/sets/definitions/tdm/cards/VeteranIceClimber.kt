package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Veteran Ice Climber — Tarkir: Dragonstorm #64
 * {1}{U} · Creature — Human Scout · 1/3
 *
 * Vigilance
 * This creature can't be blocked.
 * Whenever this creature attacks, up to one target player mills cards equal to this
 * creature's power.
 *
 * Vigilance is a keyword; "can't be blocked" is the static [AbilityFlag.CANT_BE_BLOCKED].
 * The attack trigger declares an optional ("up to one") [TargetPlayer], and mills that
 * player a number of cards equal to this creature's current power
 * ([DynamicAmounts.sourcePower], read at resolution). With no target chosen the trigger
 * simply does nothing.
 */
val VeteranIceClimber = card("Veteran Ice Climber") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 3
    oracleText = "Vigilance\n" +
        "This creature can't be blocked.\n" +
        "Whenever this creature attacks, up to one target player mills cards equal to this creature's power. " +
        "(They put that many cards from the top of their library into their graveyard.)"

    keywords(Keyword.VIGILANCE)
    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.Attacks
        target("up to one target player", TargetPlayer(optional = true))
        effect = LibraryPatterns.mill(DynamicAmounts.sourcePower(), EffectTarget.ContextTarget(0))
        description = "Whenever this creature attacks, up to one target player mills cards equal to this creature's power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcccfd7b-2846-4552-a89a-2b868bc9ab20.jpg?1743204215"
    }
}
