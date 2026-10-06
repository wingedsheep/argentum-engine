package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kessig Cagebreakers
 * {4}{G}
 * Creature — Human Rogue
 * 3/4
 *
 * Whenever this creature attacks, create a 2/2 green Wolf creature token that's tapped and
 * attacking for each creature card in your graveyard.
 *
 * The count is a [DynamicAmounts.creatureCardsInYourGraveyard] read when the trigger resolves,
 * per the ruling.
 */
val KessigCagebreakers = card("Kessig Cagebreakers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature attacks, create a 2/2 green Wolf creature token that's tapped and " +
        "attacking for each creature card in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = DynamicAmounts.creatureCardsInYourGraveyard(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            tapped = true,
            attacking = true,
            imageUri = "https://cards.scryfall.io/normal/front/a/5/a53f8031-aaa8-424c-929a-5478538a8cc6.jpg?1783940880",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Wayne England"
        flavorText = "\"They put bars on these noble beasts and then wonder why werewolves target our towns.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fae22886-da03-49f2-873c-98a7ea2ee17d.jpg?1783940916"
        ruling("2011-09-22", "You count the number of creature cards in your graveyard when the triggered ability resolves.")
        ruling("2011-09-22", "Although the tokens are attacking, they were never declared as attacking creatures (for purposes of abilities that trigger whenever a creature attacks, for example).")
    }
}
