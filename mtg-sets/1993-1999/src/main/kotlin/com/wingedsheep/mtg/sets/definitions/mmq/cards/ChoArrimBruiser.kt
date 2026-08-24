package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Cho-Arrim Bruiser
 * {5}{W}
 * Creature — Ogre Rebel
 * 3 / 4
 *
 * "Up to two target creatures" is the target requirement's own `count`/`optional`; the effect only
 * says "tap each of them" ([Effects.TapEachTarget], a `ForEach` over the chosen targets), so the
 * number never gets duplicated on the effect. The printed "you may" is the ability's `optional`.
 */
val ChoArrimBruiser = card("Cho-Arrim Bruiser") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Ogre Rebel"
    oracleText = "Whenever this creature attacks, you may tap up to two target creatures."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target("target", TargetCreature(count = 2, optional = true))
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Paolo Parente"
        flavorText = "He doesn't know why the Cho-Arrim fight the Mercadians, but he's happy to bash heads for them anyway."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26e98f06-ad8d-4a93-8ae6-3da42b63b5b5.jpg"
    }
}
