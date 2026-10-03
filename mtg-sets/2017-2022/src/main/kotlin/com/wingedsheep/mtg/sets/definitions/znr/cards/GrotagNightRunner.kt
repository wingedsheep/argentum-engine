package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Grotag Night-Runner
 * {2}{R}
 * Creature — Goblin Rogue
 * 2/3
 *
 * Whenever this creature deals combat damage to a player, exile the top card of your library.
 * You may play that card this turn.
 *
 * The impulse body is the shared gather → exile → grant pipeline (Moria Marauder).
 */
val GrotagNightRunner = card("Grotag Night-Runner") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature deals combat damage to a player, exile the top card of your library. " +
        "You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Caroline Gariba"
        flavorText = "Naturally inquisitive and adaptable, goblins can turn their small stature to their advantage as rogues."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0568341b-f972-407f-92ce-1b7c9ef742f6.jpg"
    }
}
