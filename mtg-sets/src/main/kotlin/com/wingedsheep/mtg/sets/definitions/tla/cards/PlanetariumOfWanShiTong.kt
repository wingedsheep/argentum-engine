package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Planetarium of Wan Shi Tong
 * {6}
 * Legendary Artifact
 * {1}, {T}: Scry 2.
 * Whenever you scry or surveil, look at the top card of your library. You may cast that
 * card without paying its mana cost. Do this only once each turn. (Look at the card
 * after you scry or surveil.)
 *
 * "Do this only once each turn" is `effectOncePerTurn = true` (CR 603.2h), and here the rider's
 * "this" matters: the ruling ties the turn's single use to the *cast*, not to the look — "once you
 * choose to cast the top card of your library, Planetarium of Wan Shi Tong's ability won't trigger
 * again that turn." So every scry or surveil keeps showing you the top card until you actually
 * cast one. The engine's lowering finds the consent gate at the tail of the composite below and
 * puts the spending gate inside it, so looking and declining costs nothing.
 *
 * Its effect is an atomic pipeline:
 *   1. [GatherCardsEffect] from the top of the library (count 1) — defaults to a private
 *      controller look ("look at the top card of your library").
 *   2. [MayEffect] wrapping [CastFromCollectionWithoutPayingCostEffect] — the optional
 *      "you may cast that card without paying its mana cost", which synthesizes the cast
 *      through the normal stack machinery so any target / X / mode prompts surface.
 */
val PlanetariumOfWanShiTong = card("Planetarium of Wan Shi Tong") {
    manaCost = "{6}"
    typeLine = "Legendary Artifact"
    oracleText = "{1}, {T}: Scry 2.\n" +
        "Whenever you scry or surveil, look at the top card of your library. You may cast " +
        "that card without paying its mana cost. Do this only once each turn. (Look at the " +
        "card after you scry or surveil.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Scry(2)
    }

    triggeredAbility {
        trigger = Triggers.WheneverYouScryOrSurveil
        effectOncePerTurn = true
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    count = DynamicAmount.Fixed(1),
                    player = Player.You,
                ),
                storeAs = "top",
            ),
            MayEffect(CastFromCollectionWithoutPayingCostEffect(from = "top")),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "259"
        artist = "Robin Olausson"
        flavorText = "It charts the movements of heavenly bodies, forecasting the fates of nations."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ebaf0bf-7aa2-469d-bdbb-0fbf6741eede.jpg?1764121913"
    }
}
