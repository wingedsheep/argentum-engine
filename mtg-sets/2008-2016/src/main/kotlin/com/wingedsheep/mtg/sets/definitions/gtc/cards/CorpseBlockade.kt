package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Corpse Blockade
 * {2}{B}
 * Creature — Zombie
 * 1/4
 * Defender
 * Sacrifice another creature: This creature gains deathtouch until end of turn.
 */
val CorpseBlockade = card("Corpse Blockade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Defender\nSacrifice another creature: This creature gains deathtouch until end of turn."
    power = 1
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Lucas Graciano"
        flavorText = "There are laws against it, but the dead have no one to complain to and the living are too frightened to investigate."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84234e51-e5d6-43d9-89d0-f0398fc6b7fd.jpg"
    }
}
