package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Faith's Reward
 * {3}{W}
 * Instant
 * Return to the battlefield all permanent cards in your graveyard that were put there from the
 * battlefield this turn.
 *
 * Gathers your graveyard's permanent cards carrying the from-the-battlefield-this-turn marker
 * (the Gleancrawler shape) and moves them onto the battlefield together (the Open the Vaults
 * shape), so Auras pick hosts among permanents already there.
 */
val FaithsReward = card("Faith's Reward") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Return to the battlefield all permanent cards in your graveyard that were put there " +
        "from the battlefield this turn."

    spell {
        effect = Effects.Pipeline {
            val fallen = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Permanent.putIntoGraveyardFromBattlefieldThisTurn(),
                )
            )
            move(fallen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Raymond Swanland"
        flavorText = "\"I thought the wind was ushering me to my final destination, but it lifted me to my " +
            "feet and I fought on.\"\n—Idrus, war priest of Thune"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/799ed076-4724-47bb-94a0-11b42a9826eb.jpg?1783940519"
        ruling(
            "2012-07-01",
            "Permanent cards that were put into your graveyard from anywhere else, such as a card that was " +
                "discarded, stay in the graveyard. Permanent cards that you gained control of and went to " +
                "another player's graveyard from the battlefield will likewise stay in the graveyard."
        )
        ruling(
            "2012-07-01",
            "You choose what an Aura card put onto the battlefield this way will enchant. You can't choose any " +
                "permanent cards entering at the same time as that Aura. If there's nothing legal for the Aura " +
                "to enchant, it stays in the graveyard."
        )
        ruling(
            "2012-07-01",
            "Permanent spells that were countered earlier in the turn never entered, so they won't return to " +
                "the battlefield because of Faith's Reward."
        )
    }
}
