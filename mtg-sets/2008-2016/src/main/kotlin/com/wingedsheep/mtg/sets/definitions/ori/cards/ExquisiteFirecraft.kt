package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Exquisite Firecraft
 * {1}{R}{R}
 * Sorcery
 *
 * Exquisite Firecraft deals 4 damage to any target.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard,
 * this spell can't be countered.
 *
 * Spell mastery is the card-level [cantBeCounteredIf], read off the spell on the stack whenever
 * something tries to counter it, with its caster as "you". Exquisite Firecraft itself is on the
 * stack at that moment, so it never counts toward its own spell mastery.
 */
val ExquisiteFirecraft = card("Exquisite Firecraft") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exquisite Firecraft deals 4 damage to any target.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "this spell can't be countered."

    cantBeCounteredIf = Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery)

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Chase Stone"
        flavorText = "Chandra's gift set her apart and others aflame."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42eca98e-a164-4f70-a0b0-7a604863f30b.jpg?1783938330"
        ruling("2021-03-19", "A spell or ability that counters spells can still target Exquisite Firecraft. When that spell or ability resolves, Exquisite Firecraft won't be countered if its spell mastery ability applies, but any additional effects of the countering spell or ability will still happen.")
        ruling("2015-06-22", "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the spell resolves to determine whether the spell mastery ability applies. The spell itself won't count because it's still on the stack as you make this check.")
    }
}
