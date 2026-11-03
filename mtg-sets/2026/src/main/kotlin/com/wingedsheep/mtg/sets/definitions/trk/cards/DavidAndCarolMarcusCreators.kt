package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * David and Carol Marcus, Creators
 * {2}{G}
 * Legendary Creature — Human Scientist
 * 1/3
 *
 * When David and Carol Marcus enter, create a tapped Planet land token named New Planet with
 * "{T}: Add one mana of any color."
 */
val DavidAndCarolMarcusCreators = card("David and Carol Marcus, Creators") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Human Scientist"
    power = 1
    toughness = 3
    oracleText = "When David and Carol Marcus enter, create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "KarrahE"
        flavorText = "Director of Project Genesis, Dr. Carol Marcus, along with her son David, revolutionized the science of terraforming. While Kirk voyaged to discover strange new worlds, his estranged family built them from scratch."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55d21155-647e-4892-b349-86b8d4e6d9b9.jpg?1793188281"
    }
}
