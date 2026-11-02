package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rise from the Grave — Magic 2010 #109 (earliest printing)
 * {4}{B}
 * Sorcery
 *
 * Put target creature card from a graveyard onto the battlefield under your control. That creature
 * is a black Zombie in addition to its other colors and types.
 *
 * The Dread Slaver shape: colour and creature type are *added* permanently (no duration), so a
 * later colour- or type-setting effect overwrites them per the 2018-07-13 rulings.
 */
val RiseFromTheGrave = card("Rise from the Grave") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put target creature card from a graveyard onto the battlefield under your control. " +
        "That creature is a black Zombie in addition to its other colors and types."

    spell {
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.PutOntoBattlefieldUnderYourControl(creatureCard) then
            Effects.AddColor(Color.BLACK, creatureCard, Duration.Permanent) then
            Effects.AddCreatureType("Zombie", creatureCard, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Vance Kovacs"
        flavorText = "\"Death is no excuse for disobedience.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb9dd0d9-8e35-4a8c-af6f-83c7d2a3ea7d.jpg?1783942379"
        ruling("2018-07-13", "Rise from the Grave doesn’t overwrite any previous colors or types. Rather, it adds another color and another subtype.")
        ruling("2018-07-13", "If the creature is normally colorless, it will simply become black. It can’t be both black and colorless.")
        ruling("2018-07-13", "A later effect that changes the affected creature’s colors will overwrite that part of Rise from the Grave’s effect; the creature will be only the new color. The same is true about an effect that changes the affected creature’s types or subtypes.")
    }
}
