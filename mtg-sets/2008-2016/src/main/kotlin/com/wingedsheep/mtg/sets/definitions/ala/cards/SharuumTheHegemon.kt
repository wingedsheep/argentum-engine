package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sharuum the Hegemon
 * {3}{W}{U}{B}
 * Legendary Artifact Creature — Sphinx
 * 5/5
 * Flying
 * When Sharuum enters, you may return target artifact card from your graveyard to the battlefield.
 *
 * The named target is a [TargetObject] over [TargetFilter] of `GameObjectFilter.Artifact.ownedByYou()`
 * scoped to [Zone.GRAVEYARD], and the recursion is
 * [Effects.PutOntoBattlefieldFromGraveyard] — the `fromZone`-guarded sibling of
 * `PutOntoBattlefield`, so the move is skipped if the card has left the graveyard by resolution.
 * `optional = true` lowers the printed "you may" into a
 * [com.wingedsheep.sdk.scripting.effects.Gate.MayDecide] gate around it.
 */
val SharuumTheHegemon = card("Sharuum the Hegemon") {
    manaCost = "{3}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Artifact Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Sharuum enters, you may return target artifact card from your graveyard to the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target",
            TargetObject(
                filter = TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        optional = true
        effect = Effects.PutOntoBattlefieldFromGraveyard(t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "194"
        artist = "Izzy"
        flavorText = "To gain audience with the hegemon, one must bring a riddle she has not heard."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6589eaa8-95ec-4c97-8155-185487560ae6.jpg"
        ruling("2020-08-07", "If Sharuum the Hegemon is put into your graveyard as a state-based action immediately after Sharuum the Hegemon enters the battlefield (most likely due to the \"legend rule\") it can be the artifact card targeted by its own ability.")
    }
}
