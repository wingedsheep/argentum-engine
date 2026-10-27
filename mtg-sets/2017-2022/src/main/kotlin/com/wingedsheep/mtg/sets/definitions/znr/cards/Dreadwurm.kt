package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreadwurm — Zendikar Rising #100
 * {4}{B} · Creature — Wurm Horror · 5/4
 *
 * Landfall — Whenever a land you control enters, this creature gains indestructible until end of
 * turn.
 */
val Dreadwurm = card("Dreadwurm") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wurm Horror"
    oracleText = "Landfall — Whenever a land you control enters, this creature gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Lius Lasahido"
        flavorText = "Its silent slithering belies its bulk."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/339736a8-a00f-4e21-8d92-38486f73dba9.jpg"
    }
}
