package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Professional Face-Breaker
 * {2}{R}
 * Creature — Human Warrior
 * 2/3
 *
 * Menace
 * Whenever one or more creatures you control deal combat damage to a player, create a Treasure token.
 * Sacrifice a Treasure: Exile the top card of your library. You may play that card this turn.
 *
 * The combat trigger is the batch shape (Nature's Will): once per damaged player per damage step.
 * The impulse body is the shared gather → exile → grant pipeline (Grotag Night-Runner).
 */
val ProfessionalFaceBreaker = card("Professional Face-Breaker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever one or more creatures you control deal combat damage to a player, create a Treasure token.\n" +
        "Sacrifice a Treasure: Exile the top card of your library. You may play that card this turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.CreateTreasure(1)
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact.withSubtype("Treasure"))
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42acbf52-b137-44f0-a815-2817fe8d2da2.jpg?1783923119"
    }
}
