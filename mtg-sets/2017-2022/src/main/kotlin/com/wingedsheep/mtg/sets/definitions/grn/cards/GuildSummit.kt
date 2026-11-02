package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Guild Summit — Guilds of Ravnica #41 (canonical printing)
 * {2}{U} · Enchantment
 *
 * When this enchantment enters, you may tap any number of untapped Gates you control. Draw a card
 * for each Gate tapped this way.
 * Whenever a Gate you control enters, draw a card.
 *
 * Marshaling the Troops' shape: the untapped Gates are gathered and chosen on resolution ("any
 * number" admits zero), tapped, and the draw count is the collection actually tapped.
 */
val GuildSummit = card("Guild Summit") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may tap any number of untapped Gates you control. " +
        "Draw a card for each Gate tapped this way.\n" +
        "Whenever a Gate you control enters, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val untappedGates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Land.withSubtype("Gate").youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = untappedGates,
                prompt = "You may tap any number of untapped Gates you control (draw a card for each)",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.DrawCards(tapped.count))
        }
        description = "When this enchantment enters, you may tap any number of untapped Gates you control. " +
            "Draw a card for each Gate tapped this way."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.withSubtype("Gate").youControl()).enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Sidharth Chaturvedi"
        flavorText = "In a climate of suspicion, Isperia gathered the guilds to propose a radical idea: cooperation."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d952259f-9e0a-49af-9dfb-3cad1a1bb3a8.jpg?1783934187"
    }
}
