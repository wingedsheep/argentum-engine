package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vulturous Aven
 * {3}{B}
 * Creature — Bird Shaman
 * 2/3
 * Flying
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, you draw two cards and you lose 2 life.
 *
 * The untargeted payoff rides the exploit reflexive (as with Minister of Pain), so it survives the
 * Aven exploiting itself.
 */
val VulturousAven = card("Vulturous Aven") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird Shaman"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, you draw two cards and you lose 2 life."

    keywords(Keyword.FLYING)

    exploit(onExploit = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2fb0cc9-aebd-4bbf-8735-add3b753c118.jpg"
    }
}
