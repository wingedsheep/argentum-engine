package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baleful Mastery — Strixhaven: School of Mages #64 (canonical printing)
 * {3}{B} · Instant
 *
 * You may pay {1}{B} rather than pay this spell's mana cost.
 * If the {1}{B} cost was paid, an opponent draws a card.
 * Exile target creature or planeswalker.
 *
 * The {1}{B} is the card's own [SelfAlternativeCost]; [Conditions.SelfAlternativeCostWasPaid]
 * reads whether the spell was cast for it (a copy answers as its original did). The opponent is
 * chosen as the spell resolves (ruling), so it is a resolution-time [Effects.ChooseOpponent].
 */
val BalefulMastery = card("Baleful Mastery") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You may pay {1}{B} rather than pay this spell's mana cost.\n" +
        "If the {1}{B} cost was paid, an opponent draws a card.\n" +
        "Exile target creature or planeswalker."

    selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{1}{B}"))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            Conditions.SelfAlternativeCostWasPaid,
            Effects.ChooseOpponent("Choose an opponent to draw a card") then
                Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ChosenOpponent))
        ) then Effects.Exile(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35f1a6ba-e46f-44fb-93f4-fb883d677b36.jpg?1783927370"
        ruling("2021-04-16", "If you choose to pay one alternative cost, you can't pay any other alternative costs. For example, if an effect lets you cast a \"Mastery\" spell \"without paying its mana cost,\" you can't also choose to pay its given alternative cost.")
        ruling("2021-04-16", "If an effect increases or decreases the cost of spells you cast, that cost increase or decrease is applied to the alternative cost you chose to pay. In that case, the cost was still paid for the purposes of the effect, even if you paid more or less for it when it was cast.")
        ruling("2021-04-16", "If you copy a \"Mastery\" spell and the alternative cost was paid, the copy will resolve as though the cost was paid.")
        ruling("2021-04-16", "In a multiplayer game, you choose which opponent takes the prescribed action as the spell resolves.")
    }
}
