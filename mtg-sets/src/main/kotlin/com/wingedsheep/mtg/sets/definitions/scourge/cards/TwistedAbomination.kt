package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Twisted Abomination
 * {5}{B}
 * Creature — Zombie Mutant
 * 5/3
 * {B}: Regenerate Twisted Abomination.
 * Swampcycling {2} ({2}, Discard this card: Search your library for a Swamp card,
 * reveal it, put it into your hand, then shuffle.)
 */
val TwistedAbomination = card("Twisted Abomination") {
    manaCost = "{5}{B}"
    typeLine = "Creature — Zombie Mutant"
    power = 5
    toughness = 3
    oracleText = "{B}: Regenerate Twisted Abomination.\nSwampcycling {2}"

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = RegenerateEffect(EffectTarget.Self)
    }

    keywordAbility(KeywordAbility.Typecycling("Swamp", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Daren Bader"
        flavorText = "It oozes through the swamps of Otaria, absorbing and discarding body parts."
        imageUri = "https://cards.scryfall.io/large/front/4/4/446e672f-87aa-4308-98bb-d00548c5bcef.jpg?1562528106"
    }
}
