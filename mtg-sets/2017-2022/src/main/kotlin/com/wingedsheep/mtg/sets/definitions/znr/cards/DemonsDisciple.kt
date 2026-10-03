package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demon's Disciple
 * {2}{B}
 * Creature — Human Cleric
 * 3/1
 * When this creature enters, each player sacrifices a creature or planeswalker of their choice.
 *
 * Plaguecrafter without the discard rider. Demon's Disciple itself is a legal choice for its
 * controller.
 */
val DemonsDisciple = card("Demon's Disciple") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, each player sacrifices a creature or planeswalker of their choice."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, 1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Kieran Yanner"
        flavorText = "Taborax's cultists are the latest in a long history of beings who have desired to see Zendikar torn to pieces."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/836f5197-e42b-45e0-8170-1b0a8a5beebd.jpg"
    }
}
