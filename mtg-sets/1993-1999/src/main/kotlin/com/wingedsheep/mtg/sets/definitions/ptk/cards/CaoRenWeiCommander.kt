package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cao Ren, Wei Commander
 * {2}{B}{B}
 * Legendary Creature — Human Soldier Warrior
 */
val CaoRenWeiCommander = card("Cao Ren, Wei Commander") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 3
    toughness = 3
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "When Cao Ren enters, you lose 3 life."

    keywords(Keyword.HORSEMANSHIP)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.LoseLife(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Junko Taguchi"
        flavorText = "Cao Cao's cousin, Cao Ren was known throughout the three kingdoms as the fiercest of warriors."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f7e8366-82ba-4df0-b9df-7d0aa9b972eb.jpg"
    }
}
