package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Caller of the Claw
 * {2}{G}
 * Creature — Elf
 * 2/2
 * Flash
 * When Caller of the Claw enters the battlefield, create a 2/2 green Bear creature
 * token for each nontoken creature put into your graveyard from the battlefield this turn.
 */
val CallerOfTheClaw = card("Caller of the Claw") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "Flash\nWhen Caller of the Claw enters, create a 2/2 green Bear creature token for each nontoken creature put into your graveyard from the battlefield this turn."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = DynamicAmount.NonTokenCreaturesDiedThisTurn(Player.You),
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Bear"),
            imageUri = "https://cards.scryfall.io/normal/front/7/7/772dac39-269b-4a35-aad3-320279af833f.jpg?1675455454"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a073459e-1f00-47e0-a1b3-d30203aa35d1.jpg?1562927399"
    }
}
