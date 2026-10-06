package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orcish Artillery
 * {1}{R}{R}
 * Creature — Orc Warrior
 * 1/3
 * {T}: This creature deals 2 damage to any target and 3 damage to you.
 */
val OrcishArtillery = card("Orcish Artillery") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Warrior"
    oracleText = "{T}: This creature deals 2 damage to any target and 3 damage to you."
    power = 1
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        val target = target(Targets.Any)
        effect = Effects.DealDamage(2, target) then Effects.DealDamage(3, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Anson Maddocks"
        flavorText = "In a rare display of ingenuity, the Orcs invented an incredibly destructive weapon. Most Orcish artillerists are those who dared criticize its effectiveness."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a97208b1-a91b-4129-8a00-2f97b418accc.jpg?1783948683"
    }
}
