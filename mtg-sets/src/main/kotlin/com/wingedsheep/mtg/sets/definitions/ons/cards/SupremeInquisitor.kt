package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Supreme Inquisitor
 * {3}{U}{U}
 * Creature — Human Wizard
 * 1/3
 * Tap five untapped Wizards you control: Search target player's library for up to five cards
 * and exile them. Then that player shuffles.
 */
val SupremeInquisitor = card("Supreme Inquisitor") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Tap five untapped Wizards you control: Search target player's library for up to five cards and exile them. Then that player shuffles."

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Wizard"))
        target = Targets.Player
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.ContextPlayer(0), GameObjectFilter.Any),
                name = "searchable"
            )
            val exiled = chooseUpTo(
                5, from = searchable,
                chooser = Chooser.Controller,
                name = "exiled"
            )
            move(
                exiled,
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
            )
            run(ShuffleLibraryEffect(EffectTarget.ContextTarget(0)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "rk post"
        flavorText = "\"It's hard to fight on an empty mind.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867de3d2-2178-4931-823e-ff439e1a45ea.jpg?1562927468"
    }
}
