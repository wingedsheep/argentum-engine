package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atarka, World Render
 * {5}{R}{G}
 * Legendary Creature — Dragon
 * 6/4
 * Flying, trample
 * Whenever a Dragon you control attacks, it gains double strike until end of turn.
 *
 * A per-attacker ANY-bound attack trigger (one instance per attacking Dragon, Atarka included);
 * "it" is the attacker, handed on as the triggering entity.
 */
val AtarkaWorldRender = card("Atarka, World Render") {
    manaCost = "{5}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Dragon"
    power = 6
    toughness = 4
    oracleText = "Flying, trample\n" +
        "Whenever a Dragon you control attacks, it gains double strike until end of turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Karl Kopinski"
        flavorText = "\"Her hunger knows no limit. Left unchecked, she would feast on all of Tarkir.\"\n—Yasova Dragonclaw"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c341df3-177c-49b4-91fe-38f73c64ef88.jpg?1783938674"
    }
}
