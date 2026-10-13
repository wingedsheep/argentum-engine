package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vohar, Vodalian Desecrator
 * {U}{B}
 * Legendary Creature — Phyrexian Merfolk Wizard
 * 1/2
 *
 * {T}: Draw a card, then discard a card. If you discarded an instant or sorcery card this way, each
 * opponent loses 1 life and you gain 1 life.
 * {2}, Sacrifice Vohar: You may cast target instant or sorcery card from your graveyard this turn. If
 * that spell would be put into your graveyard, exile it instead. Activate only as a sorcery.
 *
 * - The loot keeps the discarded card in a collection so "if you discarded an instant or sorcery card
 *   this way" reads the card actually discarded, not the hand.
 * - The graveyard cast is a this-turn may-play permission over the targeted card where it lies (the
 *   Zul Ashur shape — the card is not moved), with the `insteadOfGraveyard = EXILE` rider. Costs and
 *   timing are paid and followed normally (ruling).
 */
val VoharVodalianDesecrator = card("Vohar, Vodalian Desecrator") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Phyrexian Merfolk Wizard"
    power = 1
    toughness = 2
    oracleText = "{T}: Draw a card, then discard a card. If you discarded an instant or sorcery card this way, " +
        "each opponent loses 1 life and you gain 1 life.\n" +
        "{2}, Sacrifice Vohar: You may cast target instant or sorcery card from your graveyard this turn. If " +
        "that spell would be put into your graveyard, exile it instead. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseExactly(1, from = hand, prompt = "Discard a card")
            discard(discarded)
            ifNotEmpty(discarded, filter = GameObjectFilter.InstantOrSorcery) {
                run(
                    Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(1)
                )
            }
        }
        description = "Draw a card, then discard a card. If you discarded an instant or sorcery card this way, " +
            "each opponent loses 1 life and you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(
                Effects.GrantMayPlayFromExile(
                    from = chosen,
                    expiry = MayPlayExpiry.EndOfTurn,
                    insteadOfGraveyard = AfterResolveDestination.EXILE,
                )
            )
        }
        timing = TimingRule.SorcerySpeed
        description = "You may cast target instant or sorcery card from your graveyard this turn. If that " +
            "spell would be put into your graveyard, exile it instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/807c7812-c108-45a7-a5b2-08cc693bc500.jpg?1783921272"
        ruling("2022-09-09", "You must pay all costs and follow all normal timing rules for cards cast with Vohar, Vodalian Desecrator's last ability.")
    }
}
