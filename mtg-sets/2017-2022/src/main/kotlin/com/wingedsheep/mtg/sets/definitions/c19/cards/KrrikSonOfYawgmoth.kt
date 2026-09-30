package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PayLifeForColoredMana
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * K'rrik, Son of Yawgmoth
 * {4}{B/P}{B/P}{B/P}
 * Legendary Creature — Phyrexian Horror Minion
 * 2/2
 *
 * ({B/P} can be paid with either {B} or 2 life.)
 * Lifelink
 * For each {B} in a cost, you may pay 2 life rather than pay that mana.
 * Whenever you cast a black spell, put a +1/+1 counter on K'rrik.
 *
 * The substitution is [PayLifeForColoredMana]: every {B} (and the black half of a hybrid) in a cost
 * K'rrik's controller pays becomes payable with 2 life, reusing the Phyrexian payment path. Mana
 * value is unchanged (always 7 for K'rrik itself).
 */
val KrrikSonOfYawgmoth = card("K'rrik, Son of Yawgmoth") {
    manaCost = "{4}{B/P}{B/P}{B/P}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Horror Minion"
    power = 2
    toughness = 2
    oracleText = "({B/P} can be paid with either {B} or 2 life.)\n" +
        "Lifelink\n" +
        "For each {B} in a cost, you may pay 2 life rather than pay that mana.\n" +
        "Whenever you cast a black spell, put a +1/+1 counter on K'rrik."

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = PayLifeForColoredMana(Color.BLACK)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.BLACK))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3592fbe4-8588-486e-99ba-c327b0b6ba24.jpg?1783932811"
    }
}
