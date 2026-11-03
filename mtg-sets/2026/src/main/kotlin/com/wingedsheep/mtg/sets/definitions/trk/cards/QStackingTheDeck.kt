package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Q, Stacking the Deck
 * {2}{R}{R}
 * Legendary Creature — Q
 * 4/4
 *
 * When Q enters, shuffle your library, then exile cards from the top of your library until you
 * exile a nonland card. That nonland card gains "Exile a card from your hand: Create a copy of
 * this card. You may cast the copy. Activate only during your turn and only if this card is
 * exiled."
 *
 * The gained ability is activatable from exile (`activateFromZone = EXILE`), so the grant lands on
 * the exiled card itself and ends when that card leaves exile (CR 400.7). The copy is cast paying
 * its costs; a copy not cast ceases to exist.
 */
val QStackingTheDeck = card("Q, Stacking the Deck") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Q"
    power = 4
    toughness = 4
    oracleText = "When Q enters, shuffle your library, then exile cards from the top of your library " +
        "until you exile a nonland card. That nonland card gains \"Exile a card from your hand: Create " +
        "a copy of this card. You may cast the copy. Activate only during your turn and only if this " +
        "card is exiled.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Effects.ShuffleLibrary())
            val found = gatherUntilMatch(GameObjectFilter.Nonland)
            exile(found.revealed)
            run(
                Effects.GrantActivatedAbility(
                    ability = ActivatedAbility(
                        cost = Costs.ExileFromHand(),
                        effect = Effects.Pipeline {
                            val copy = copyCard(EffectTarget.Self)
                            run(Effects.May(Effects.CastFromCollection(copy), descriptionOverride = "You may cast the copy."))
                        },
                        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn),
                        activateFromZone = Zone.EXILE,
                        descriptionOverride = "Exile a card from your hand: Create a copy of this card. You may " +
                            "cast the copy. Activate only during your turn and only if this card is exiled.",
                    ),
                    target = found.match.asTarget,
                    duration = Duration.Permanent,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "165"
        artist = "Ryan Valle"
        flavorText = "\"Games? Did someone say games?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcbe34db-0065-4960-92b8-3ea1d336e803.jpg?1792508237"
    }
}
