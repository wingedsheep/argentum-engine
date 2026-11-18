package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Daring Thief — Journey into Nyx #36
 * {2}{U} · Creature — Human Rogue · 2/3
 *
 * Inspired — Whenever this creature becomes untapped, you may exchange control of target nonland
 * permanent you control and target permanent an opponent controls that shares a card type with it.
 *
 * Same shape as Gauntlets of Chaos: the second target reads the first through dependent target
 * selection ([GameObjectFilter.sharingCardTypeWith], any card type, projected), so an artifact you
 * control can be swapped for an opponent's artifact land. Both targets are re-checked on
 * resolution; if either is illegal the exchange doesn't happen. "You may" is decided on
 * resolution, and the control change is permanent (it outlives the Thief).
 */
val DaringThief = card("Daring Thief") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, you may exchange control of target " +
        "nonland permanent you control and target permanent an opponent controls that shares a card type with it."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        val yours = target(TargetFilter(GameObjectFilter.NonlandPermanent.youControl()))
        val theirs = target(
            TargetFilter(GameObjectFilter.Permanent.opponentControls().sharingCardTypeWith(yours))
        )
        effect = Effects.May(Effects.ExchangeControl(yours, theirs))
        description = "Inspired — Whenever this creature becomes untapped, you may exchange control of target " +
            "nonland permanent you control and target permanent an opponent controls that shares a card type with it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Johann Bodin"
        flavorText = "Honesty is the first casualty of war."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9d9378d-e553-48b5-8a23-59a35329eda2.jpg?1783939448"

        ruling("2014-04-26", "The exchange of control lasts indefinitely. It doesn't expire when Daring Thief leaves the battlefield.")
        ruling("2014-04-26", "If the full exchange can't happen, perhaps because one of the targets is illegal as the inspired ability tries to resolve, then nothing happens. No permanents change controllers.")
        ruling("2014-04-26", "Although the permanent you control can't be a land, the other target can, provided it shares a card type with the first target. For example, you could target an artifact you control and an artifact land controlled by an opponent.")
    }
}
