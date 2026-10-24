package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Devastating Mastery — Strixhaven: School of Mages #14 (canonical printing)
 * {2}{W}{W}{W}{W} · Sorcery
 *
 * You may pay {2}{W}{W} rather than pay this spell's mana cost.
 * If the {2}{W}{W} cost was paid, an opponent chooses up to two nonland permanents they control
 * and returns them to their owner's hand.
 * Destroy all nonland permanents.
 *
 * The {2}{W}{W} is the card's own [SelfAlternativeCost]; [Conditions.SelfAlternativeCostWasPaid]
 * gates the bounce. The opponent is chosen as the spell resolves (ruling), and that same
 * [Chooser.ChosenOpponent] picks up to two of *their own* nonland permanents — gathered from
 * [Player.ChosenOpponent]'s battlefield — which go back to their owner's hand before the wrath.
 */
val DevastatingMastery = card("Devastating Mastery") {
    manaCost = "{2}{W}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You may pay {2}{W}{W} rather than pay this spell's mana cost.\n" +
        "If the {2}{W}{W} cost was paid, an opponent chooses up to two nonland permanents they control " +
        "and returns them to their owner's hand.\n" +
        "Destroy all nonland permanents."

    selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{2}{W}{W}"))

    spell {
        effect = Effects.If(
            Conditions.SelfAlternativeCostWasPaid,
            Effects.Pipeline {
                run(Effects.ChooseOpponent("Choose an opponent to return up to two nonland permanents"))
                val theirs = gather(
                    CardSource.ControlledPermanents(
                        player = Player.ChosenOpponent,
                        filter = GameObjectFilter.NonlandPermanent
                    )
                )
                val saved = chooseUpTo(
                    2,
                    from = theirs,
                    chooser = Chooser.ChosenOpponent,
                    prompt = "Choose up to two nonland permanents you control to return to their owner's hand",
                    selectedLabel = "Return to hand",
                    useTargetingUI = true
                )
                toHand(saved)
            }
        ) then Effects.DestroyAll(GameObjectFilter.NonlandPermanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b6be019-29b9-4c81-9899-aefeaf9cc977.jpg?1783927392"
        ruling("2021-04-16", "If you choose to pay one alternative cost, you can't pay any other alternative costs. For example, if an effect lets you cast a \"Mastery\" spell \"without paying its mana cost,\" you can't also choose to pay its given alternative cost.")
        ruling("2021-04-16", "If an effect increases or decreases the cost of spells you cast, that cost increase or decrease is applied to the alternative cost you chose to pay. In that case, the cost was still paid for the purposes of the effect, even if you paid more or less for it when it was cast.")
        ruling("2021-04-16", "If you copy a \"Mastery\" spell and the alternative cost was paid, the copy will resolve as though the cost was paid.")
        ruling("2021-04-16", "In a multiplayer game, you choose which opponent takes the prescribed action as the spell resolves.")
    }
}
