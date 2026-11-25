package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Street Sweeper — Return to Ravnica #234 (canonical printing, only printing)
 * {6} · Artifact Creature — Construct · 4/6
 *
 * Whenever this creature attacks, destroy all Auras attached to target land.
 *
 * [CardSource.AttachedTo] gathers every Aura on the targeted land at resolution, then the
 * pipeline destroys them all at once. Any land is a legal target, including one with no Auras
 * (the ability is mandatory — see the ruling); the land itself is untouched.
 */
val StreetSweeper = card("Street Sweeper") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 6
    oracleText = "Whenever this creature attacks, destroy all Auras attached to target land."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val land = target(TargetFilter.Land)
        effect = Effects.Pipeline {
            val auras = gather(
                CardSource.AttachedTo(
                    host = land,
                    filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA),
                )
            )
            destroy(auras)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Izzy"
        flavorText = "The sweepers roam endlessly, scrubbing away the refuse—and anything else too slow or too clumsy to get out of their way."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a62827a-183f-4bef-b6ce-20a4577f6d30.jpg?1783940323"
        ruling("2012-10-01", "Street Sweeper's ability is mandatory, but you can choose a land with no Auras attached to it as the target.")
    }
}
