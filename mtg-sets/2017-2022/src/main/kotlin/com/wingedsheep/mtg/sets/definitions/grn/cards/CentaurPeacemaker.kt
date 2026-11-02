package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Centaur Peacemaker
 * {1}{G}{W}
 * Creature — Centaur Cleric
 * 3/3
 *
 * When this creature enters, each player gains 4 life.
 */
val CentaurPeacemaker = card("Centaur Peacemaker") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Centaur Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, each player gains 4 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(4, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Raoul Vitale"
        flavorText = "\"Please accept this offering. I sincerely hope to leave my mace at my side.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e630e9b-7817-4255-9de1-795b2a788d80.jpg?1783934141"
    }
}
