package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Worldfire
 * {6}{R}{R}{R}
 * Sorcery
 * Exile all permanents. Exile all cards from all hands and graveyards. Each player's life total
 * becomes 1.
 *
 * The three steps run in printed order within one resolution. Worldfire itself is on the stack, not
 * in a hand or graveyard, so it is not exiled — it goes to its owner's graveyard as the last part of
 * resolving.
 */
val Worldfire = card("Worldfire") {
    manaCost = "{6}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile all permanents. Exile all cards from all hands and graveyards. " +
        "Each player's life total becomes 1."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllPermanents,
            effect = Effects.Move(EffectTarget.IterationEntity, Zone.EXILE)
        ) then Effects.ForEachPlayer(
            players = Player.Each,
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND))
                exile(hand)
                val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD))
                exile(graveyard)
            }
        ) then Effects.ForEachPlayer(
            players = Player.Each,
            Effects.SetLifeTotal(1, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "158"
        artist = "Izzy"
        flavorText = "Even the smallest spark may set a world ablaze."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ef3d4b5-0453-4bf0-b018-23b0c3b9ae11.jpg?1783940477"
        ruling("2012-07-01", "These actions are performed in order, but no triggered abilities resolve in between them and no player may cast spells or activate abilities in between them. This means that if a permanent's ability triggers on a player losing life, that permanent will be exiled first and won't be on the battlefield when your life total becomes 1. Leaves-the-battlefield triggers won't resolve until you're done resolving Worldfire entirely.")
        ruling("2012-07-01", "If your life total is greater than 1 when Worldfire resolves, the change in life total will count as life loss. Other effects that interact with life loss will interact with this effect accordingly.")
        ruling("2012-07-01", "Worldfire won't be exiled. It will go to your graveyard as the final part of its resolution.")
    }
}
