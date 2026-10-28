package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zombie Apocalypse
 * {3}{B}{B}{B}
 * Sorcery
 * Return all Zombie creature cards from your graveyard to the battlefield tapped, then destroy all Humans.
 *
 * The return happens first, so a returned card that is both a Zombie and a Human is then
 * destroyed (2011 ruling). "All Humans" spans every player's permanents.
 */
val ZombieApocalypse = card("Zombie Apocalypse") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return all Zombie creature cards from your graveyard to the battlefield tapped, then destroy all Humans."

    spell {
        effect = Effects.Pipeline {
            val zombies = gather(
                CardSource.FromZone(
                    Zone.GRAVEYARD,
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE)
                )
            )
            move(zombies, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        } then Effects.DestroyAll(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Peter Mohrbacher"
        flavorText = "\"There will come a day so dark you will pray for death. On that day your prayers will be answered.\"\n—Minaldra, the Vizag Atum"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe662a08-a8b1-4f25-b7c0-dca1c7ad7271.jpg?1783940823"
        ruling("2011-01-22", "All Humans are destroyed, not just ones you control.")
        ruling(
            "2011-01-22",
            "If a creature card in your graveyard is both a Zombie and a Human, it will be returned to the battlefield tapped, then be destroyed."
        )
    }
}
