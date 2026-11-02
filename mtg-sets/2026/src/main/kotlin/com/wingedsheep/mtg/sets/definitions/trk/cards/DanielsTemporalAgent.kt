package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Daniels, Temporal Agent
 * {2}{B/G}{B/G}
 * Legendary Creature — Human Advisor
 * 3/1
 *
 * When Daniels enters, return target creature card from your graveyard to your hand.
 * {2}, Put this card from your graveyard on the bottom of your library: You gain 2 life.
 *
 * The second ability works only from the graveyard (`activateFromZone = GRAVEYARD`); putting the
 * card on the bottom of its owner's library is part of the cost, so it is paid before the ability
 * goes on the stack.
 */
val DanielsTemporalAgent = card("Daniels, Temporal Agent") {
    manaCost = "{2}{B/G}{B/G}"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 1
    oracleText = "When Daniels enters, return target creature card from your graveyard to your hand.\n" +
        "{2}, Put this card from your graveyard on the bottom of your library: You gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.HAND)
        description = "Return target creature card from your graveyard to your hand."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.PutSelfOnBottomOfLibrary)
        effect = Effects.GainLife(2)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}, Put this card from your graveyard on the bottom of your library: You gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Kaitlyn McCulley"
        flavorText = "\"You're thinking of time travel like we're in some H.G. Wells novel. We're not. " +
            "It's far more complicated.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b64dd792-7dc7-4d22-9776-9b9e25e1173d.jpg?1792508309"
    }
}
