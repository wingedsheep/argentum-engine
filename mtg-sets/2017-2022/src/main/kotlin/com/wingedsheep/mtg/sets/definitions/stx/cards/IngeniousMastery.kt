package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ingenious Mastery — Strixhaven: School of Mages #44 (canonical printing)
 * {X}{2}{U} · Sorcery
 *
 * You may pay {2}{U} rather than pay this spell's mana cost.
 * If the {2}{U} cost was paid, you draw three cards, then an opponent creates two Treasure tokens
 * and they scry 2. If that cost wasn't paid, you draw X cards.
 *
 * The {2}{U} is the card's own [SelfAlternativeCost] (it has no {X}, so X is 0 when it's paid);
 * [Conditions.SelfAlternativeCostWasPaid] picks the branch. The opponent is chosen as the spell
 * resolves (ruling) and both the Treasures and the scry go to that [Player.ChosenOpponent].
 */
val IngeniousMastery = card("Ingenious Mastery") {
    manaCost = "{X}{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You may pay {2}{U} rather than pay this spell's mana cost.\n" +
        "If the {2}{U} cost was paid, you draw three cards, then an opponent creates two Treasure tokens and they scry 2. " +
        "If that cost wasn't paid, you draw X cards."

    selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{2}{U}"))

    spell {
        val opponent = EffectTarget.PlayerRef(Player.ChosenOpponent)
        effect = Effects.If(
            Conditions.SelfAlternativeCostWasPaid,
            then = Effects.DrawCards(3) then
                Effects.ChooseOpponent("Choose an opponent to create two Treasure tokens and scry 2") then
                Effects.CreateTreasure(2, controller = opponent) then
                Effects.Scry(2, opponent),
            otherwise = Effects.DrawCards(DynamicAmounts.xValue())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2386d020-a9ae-4d54-b5e4-f2cb5a7298cc.jpg?1783927380"
        ruling("2021-04-16", "If you choose to pay one alternative cost, you can't pay any other alternative costs. For example, if an effect lets you cast a \"Mastery\" spell \"without paying its mana cost,\" you can't also choose to pay its given alternative cost.")
        ruling("2021-04-16", "If an effect increases or decreases the cost of spells you cast, that cost increase or decrease is applied to the alternative cost you chose to pay. In that case, the cost was still paid for the purposes of the effect, even if you paid more or less for it when it was cast.")
        ruling("2021-04-16", "If you copy a \"Mastery\" spell and the alternative cost was paid, the copy will resolve as though the cost was paid.")
        ruling("2021-04-16", "In a multiplayer game, you choose which opponent takes the prescribed action as the spell resolves.")
    }
}
