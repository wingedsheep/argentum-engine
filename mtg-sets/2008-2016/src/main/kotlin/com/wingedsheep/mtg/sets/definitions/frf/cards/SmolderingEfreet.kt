package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smoldering Efreet
 * {1}{R}
 * Creature — Efreet Monk
 * 2/2
 * When this creature dies, it deals 2 damage to you.
 */
val SmolderingEfreet = card("Smoldering Efreet") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Efreet Monk"
    oracleText = "When this creature dies, it deals 2 damage to you."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Chase Stone"
        flavorText = "The efreet are drawn to the Kaisham Wanderers, a loosely organized Jeskai school where trickery is employed to challenge the status quo and upend the belief systems of others."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af33b204-6b50-4404-b986-9f6b970a7f06.jpg"
    }
}
