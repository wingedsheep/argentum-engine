package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * Corpse Traders
 * {3}{B}
 * Creature — Human Rogue
 * 3/3
 *
 * {2}{B}, Sacrifice a creature: Target opponent reveals their hand. You choose a card from it.
 * That player discards that card. Activate only as a sorcery.
 *
 * Coercion's reveal-choose-discard pipeline on a sorcery-speed sacrifice ability.
 */
val CorpseTraders = card("Corpse Traders") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 3
    oracleText = "{2}{B}, Sacrifice a creature: Target opponent reveals their hand. You choose a card from it. " +
        "That player discards that card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Sacrifice(Filters.Creature)
        )
        timing = TimingRule.SorcerySpeed
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer), revealed = true)
            val discarded = chooseExactly(1, hand, prompt = "Choose a card for that player to discard")
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, opponent.asPlayer),
                moveType = MoveType.Discard)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Kev Walker"
        flavorText = "Those without breath can't complain."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df3eed10-7a8f-4c89-8be8-389f979e10b7.jpg?1783940704"
        ruling("2013-04-15", "If you cast this as normal during your main phase, it will enter the battlefield and you'll receive priority. If no abilities trigger because of this, you can activate its ability immediately, before any other player has a chance to remove it from the battlefield.")
        ruling("2012-05-01", "You can sacrifice Corpse Traders to pay the cost of its ability.")
    }
}
