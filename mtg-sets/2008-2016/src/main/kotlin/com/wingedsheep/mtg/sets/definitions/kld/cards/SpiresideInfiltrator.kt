package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spireside Infiltrator
 * {2}{R}
 * Creature — Human Rogue
 * 3/2
 * Whenever this creature becomes tapped, it deals 1 damage to each opponent.
 */
val SpiresideInfiltrator = card("Spireside Infiltrator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    oracleText = "Whenever this creature becomes tapped, it deals 1 damage to each opponent."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Deruchenko Alexander"
        flavorText = "\"The Consulate promised that we would all have access to aether. I'm just keeping them honest.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9551190-9764-47a6-b414-8411beec89d2.jpg?1783937188"
    }
}
