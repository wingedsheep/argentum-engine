package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Immortal Servitude
 * {X}{W/B}{W/B}{W/B}
 * Sorcery
 *
 * Return each creature card with mana value X from your graveyard to the battlefield.
 *
 * Raise the Past's mass reanimation with Rally the Ancestors' X-bound filter tightened to
 * equality ([GameObjectFilter.manaValueEqualsX], read at resolution). A creature card with X in
 * its own mana cost has that X counted as 0 in the graveyard, so it returns when X is its
 * printed mana value without X.
 */
val ImmortalServitude = card("Immortal Servitude") {
    manaCost = "{X}{W/B}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Return each creature card with mana value X from your graveyard to the battlefield."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature.manaValueEqualsX(),
                ),
            )
            move(
                creatures,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true,
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Seb McKinnon"
        flavorText = "\"The fine print of countless contracts has ensured we are never defenseless.\"\n" +
            "—Teysa Karlov, Grand Envoy of Orzhov"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88366762-4be0-422a-a0ff-ed046d30afe1.jpg?1783940095"
        ruling("2013-01-24", "If a creature card in a graveyard has X in its mana cost, that X is considered to be 0.")
    }
}
