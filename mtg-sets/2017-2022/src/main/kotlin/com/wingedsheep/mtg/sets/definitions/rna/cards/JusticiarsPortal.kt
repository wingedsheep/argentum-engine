package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Justiciar's Portal
 * {1}{W}
 * Instant
 * Exile target creature you control, then return that card to the battlefield under its owner's control.
 * It gains first strike until end of turn.
 */
val JusticiarsPortal = card("Justiciar's Portal") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return that card to the battlefield under its owner's control. " +
        "It gains first strike until end of turn."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.Move(creature, Zone.BATTLEFIELD) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Micah Epstein"
        flavorText = "With the new guildmaster's innovations, arresters can arrive on the scene moments before a crime is committed."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1df611df-3490-4b07-8034-6da9a0122a81.jpg?1783933722"
        ruling("2019-01-25", "Once the exiled creature returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled creature will be put into their owners' graveyards. Equipment attached to the exiled creature will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2019-01-25", "The creature returns untapped unless another effect causes it to enter the battlefield tapped.")
        ruling("2019-01-25", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
