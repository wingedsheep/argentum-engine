package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jadzi, Oracle of Arcavios // Journey to the Oracle — Strixhaven: School of Mages #151
 * (canonical printing)
 *
 * Modal double-faced card (CR 712): a creature front and a sorcery back (`modalBack { }`).
 *
 * Front — Jadzi ({6}{U}{U}, Legendary Creature — Human Wizard, 5/5):
 *  - "Discard a card: Return Jadzi to its owner's hand."
 *  - Magecraft: reveal the top card of your library. A land is put onto the battlefield (not a land
 *    play); a nonland card may be cast for {1} rather than its mana cost — [Effects.CastFromCollectionByPaying]
 *    with a fixed alternative mana cost, cast from the library during resolution, so timing is
 *    ignored and {X} is 0 (rulings). `chooseSpell` offers each nonland face of a modal DFC
 *    (ruling), and declining leaves the card on top.
 *
 * Back — Journey to the Oracle ({2}{G}{G}, Sorcery): put any number of lands from hand, then with
 * eight or more lands you may discard a card; if you do, the spell returns itself to hand (Hanabi
 * Blast's self-return, gated by [Effects.IfYouDo]).
 */
val JadziOracleOfArcavios = card("Jadzi, Oracle of Arcavios") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 5
    toughness = 5
    oracleText = "Discard a card: Return Jadzi to its owner's hand.\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, reveal the top card of your " +
        "library. If it's a nonland card, you may cast it by paying {1} rather than paying its mana cost. " +
        "If it's a land card, put it onto the battlefield."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "Discard a card: Return Jadzi to its owner's hand."
    }

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.Pipeline(
            descriptionOverride = "Magecraft — Reveal the top card of your library. If it's a nonland card, " +
                "you may cast it by paying {1} rather than paying its mana cost. If it's a land card, put it " +
                "onto the battlefield."
        ) {
            val top = gather(CardSource.TopOfLibrary(1), revealed = true)
            move(top, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), filter = GameObjectFilter.Land)
            val nonland = filter(top, GameObjectFilter.Nonland)
            val spell = chooseSpell(
                from = nonland,
                prompt = "You may cast the revealed card by paying {1} rather than its mana cost",
                selectedLabel = "Cast for {1}",
            )
            run(Effects.CastFromCollectionByPaying(spell, "{1}"))
        }
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, reveal the top card " +
            "of your library. If it's a nonland card, you may cast it by paying {1} rather than paying its " +
            "mana cost. If it's a land card, put it onto the battlefield."
    }

    modalBack("Journey to the Oracle") {
        manaCost = "{2}{G}{G}"
        typeLine = "Sorcery"
        oracleText = "You may put any number of land cards from your hand onto the battlefield. Then if you " +
            "control eight or more lands, you may discard a card. If you do, return Journey to the Oracle " +
            "to its owner's hand."
        imageUri = "https://cards.scryfall.io/normal/back/d/7/d7148d24-373e-4485-860b-c3429c2337f2.jpg?1783927336"
        spell {
            effect = Effects.Pipeline {
                val lands = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Land))
                val chosen = chooseAnyNumber(
                    from = lands,
                    prompt = "Choose any number of land cards to put onto the battlefield",
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            } then Effects.If(
                Conditions.ControlLandsAtLeast(8),
                Effects.May(
                    Effects.IfYouDo(
                        Patterns.Hand.discardCards(1),
                        Effects.Pipeline {
                            toHand(gather(CardSource.Entity(EffectTarget.Self)), Player.OwnerOfSource)
                        },
                    )
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7148d24-373e-4485-860b-c3429c2337f2.jpg?1783927336"
        ruling("2021-04-16", "Putting a land onto the battlefield does not count as playing a land for the turn and doesn't use the normal timing restrictions.")
        ruling("2021-04-16", "You may choose not to cast a nonland card revealed by Jadzi's triggered ability. If you don't cast it, it stays on top of your library.")
        ruling("2021-04-16", "If you're casting the card you reveal with Jadzi's ability, you ignore normal timing restrictions.")
        ruling("2021-04-16", "If you reveal a modal double-faced card with Jadzi's triggered ability, and both faces are nonland, you may cast either face by paying {1} rather than its mana cost. If only the back face is a land, you may cast the front face. If both faces are lands, you will put onto the battlefield front face up.")
        ruling("2021-04-16", "If you cast a spell with {X} in its mana cost using Jadzi's magecraft ability, you must choose 0 as the value of X.")
        ruling("2021-04-16", "Because the magecraft ability supplies an alternative cost of {1} for the spell, you cannot pay any other alternative costs that may apply. However, you may pay additional costs and must pay any additional costs that are required.")
        ruling("2021-04-16", "You may activate Jadzi's activated ability only while Jadzi is on the battlefield. If Jadzi is no longer on the battlefield as the ability resolves, it won't return her to your hand from any other zone.")
    }
}
