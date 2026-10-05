package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Twiddle = card("Twiddle") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "You may tap or untap target artifact, creature, or land."
    colorIdentity = "U"
    spell {
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land))
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(permanent)),
                    Mode.noTarget(Effects.Untap(permanent))
                ),
                countsAsModalSpell = false
            )
        )
    }

    metadata {
        ruling("2004-10-04", "This is not a toggle effect. If you use Twiddle to tap a card and before it takes effect your opponent taps it, Twiddle will not automatically untap the card.")
        ruling("2004-10-04", "The decision whether or not to tap or untap is made on resolution. This is not a modal spell.")
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/576e811f-26a3-4a7c-bd13-3b1cc3e184eb.jpg?1783948699"
    }
}
