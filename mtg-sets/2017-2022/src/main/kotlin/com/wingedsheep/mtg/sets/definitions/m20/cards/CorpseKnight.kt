package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Corpse Knight (M20 #206)
 * {W}{B}  Creature — Zombie Knight  2/2
 *
 * Whenever another creature you control enters, each opponent loses 1 life.
 */
val CorpseKnight = card("Corpse Knight") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Zombie Knight"
    oracleText = "Whenever another creature you control enters, each opponent loses 1 life."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Karl Kopinski"
        flavorText = "\"With each knight that rode out from the bone-white keep, the queen's soldiers felt their courage failing.\"\n—Krinnea, *Siege of the Bone Spire*"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b45117b-8369-48fc-8e5d-8986e662d123.jpg"
    }
}
