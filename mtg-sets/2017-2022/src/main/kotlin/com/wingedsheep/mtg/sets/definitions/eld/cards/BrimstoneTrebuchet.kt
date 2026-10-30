package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brimstone Trebuchet
 * {2}{R}
 * Artifact Creature — Wall
 * 1/3
 *
 * Defender, reach
 * {T}: This creature deals 1 damage to each opponent.
 * Whenever a Knight you control enters, untap this creature.
 */
val BrimstoneTrebuchet = card("Brimstone Trebuchet") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Wall"
    oracleText = "Defender, reach\n" +
        "{T}: This creature deals 1 damage to each opponent.\n" +
        "Whenever a Knight you control enters, untap this creature."
    power = 1
    toughness = 3

    keywords(Keyword.DEFENDER, Keyword.REACH)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Knight").youControl()).enters()
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77c49e0e-4375-4e45-a57b-8df667e45ce6.jpg?1783932629"
    }
}
