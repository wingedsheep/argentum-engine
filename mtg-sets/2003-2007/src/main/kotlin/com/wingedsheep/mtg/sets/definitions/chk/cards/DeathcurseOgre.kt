package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathcurse Ogre
 * {5}{B}
 * Creature — Ogre Warrior
 * 3/3
 * When this creature dies, each player loses 3 life.
 *
 * "Each player" includes the Ogre's controller — [Player.Each], not each opponent.
 */
val DeathcurseOgre = card("Deathcurse Ogre") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "When this creature dies, each player loses 3 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature dies, each player loses 3 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Mark Tedin"
        flavorText = "After their worship of oni began, only a few of Kamigawa's ogres remained in the bitter cold of the Tendo Peaks. Most were drawn to the darkness of Takenuma."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62bd3c73-fff4-40e8-b9db-80a010a4dd37.jpg?1783944315"
    }
}
