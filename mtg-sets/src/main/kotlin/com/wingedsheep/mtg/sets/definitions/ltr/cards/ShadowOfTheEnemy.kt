package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Shadow of the Enemy
 * {3}{B}{B}{B}
 * Sorcery
 *
 * Exile all creature cards from target player's graveyard. You may cast spells from
 * among those cards for as long as they remain exiled, and mana of any type can be
 * spent to cast them.
 */
val ShadowOfTheEnemy = card("Shadow of the Enemy") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile all creature cards from target player's graveyard. You may cast spells from among those cards for as long as they remain exiled, and mana of any type can be spent to cast them."

    spell {
        val player = target("target player", Targets.Player)
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.ContextPlayer(0), GameObjectFilter.Creature),
                storeAs = "exiledCreatures"
            ),
            MoveCollectionEffect(
                from = "exiledCreatures",
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
            ),
            GrantMayPlayFromExileEffect(
                from = "exiledCreatures",
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "107"
        artist = "Shahab Alizadeh"
        flavorText = "As Frodo put on the Ring, Sauron was suddenly aware of the magnitude of his own folly. His wrath blazed in consuming flame, but his fear rose like black smoke."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e47cab70-55cb-481c-b4cd-32a41251f210.jpg?1686968709"
    }
}
