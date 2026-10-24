package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fervent Mastery — Strixhaven: School of Mages #101 (canonical printing)
 * {3}{R}{R} · Sorcery
 *
 * You may pay {2}{R}{R} rather than pay this spell's mana cost.
 * If the {2}{R}{R} cost was paid, an opponent discards any number of cards, then draws that many
 * cards.
 * Search your library for up to three cards, put them into your hand, shuffle, then discard three
 * cards at random.
 *
 * The {2}{R}{R} is the card's own [SelfAlternativeCost]; [Conditions.SelfAlternativeCostWasPaid]
 * gates the opponent's rummage. The opponent is chosen as the spell resolves (ruling) and decides
 * how many and which cards to discard — possibly none (ruling) — then draws the number actually
 * discarded. The search is the stock up-to-three tutor to hand (shuffle included), then a random
 * discard of three.
 */
val FerventMastery = card("Fervent Mastery") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may pay {2}{R}{R} rather than pay this spell's mana cost.\n" +
        "If the {2}{R}{R} cost was paid, an opponent discards any number of cards, then draws that many cards.\n" +
        "Search your library for up to three cards, put them into your hand, shuffle, then discard three cards at random."

    selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{2}{R}{R}"))

    spell {
        effect = Effects.If(
            Conditions.SelfAlternativeCostWasPaid,
            Effects.Pipeline {
                run(Effects.ChooseOpponent("Choose an opponent to discard any number of cards, then draw that many"))
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.ChosenOpponent, GameObjectFilter.Any))
                val discarded = chooseAnyNumber(
                    from = hand,
                    chooser = Chooser.ChosenOpponent,
                    prompt = "Discard any number of cards, then draw that many cards",
                    selectedLabel = "Discard"
                )
                discard(discarded, Player.ChosenOpponent)
                run(Effects.DrawCards(discarded.count, EffectTarget.PlayerRef(Player.ChosenOpponent)))
            }
        ) then
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                count = 3,
                destination = SearchDestination.HAND
            ) then
            Patterns.Hand.discardRandom(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Sean Sevestre"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f824caf-b32c-442a-8631-887831a57360.jpg?1783927355"
        ruling("2021-04-16", "If the {2}{R}{R} cost was paid, the opponent chooses how many and which cards to discard as Fervent Mastery resolves. They may choose to discard no cards.")
        ruling("2021-04-16", "If you choose to pay one alternative cost, you can't pay any other alternative costs. For example, if an effect lets you cast a \"Mastery\" spell \"without paying its mana cost,\" you can't also choose to pay its given alternative cost.")
        ruling("2021-04-16", "If an effect increases or decreases the cost of spells you cast, that cost increase or decrease is applied to the alternative cost you chose to pay. In that case, the cost was still paid for the purposes of the effect, even if you paid more or less for it when it was cast.")
        ruling("2021-04-16", "If you copy a \"Mastery\" spell and the alternative cost was paid, the copy will resolve as though the cost was paid.")
        ruling("2021-04-16", "In a multiplayer game, you choose which opponent takes the prescribed action as the spell resolves.")
    }
}
