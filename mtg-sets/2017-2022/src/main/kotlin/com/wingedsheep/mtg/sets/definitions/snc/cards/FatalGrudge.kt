package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fatal Grudge — {B}{R}
 * Sorcery (SNC #187, uncommon)
 *
 * As an additional cost to cast this spell, sacrifice a nonland permanent.
 * Each opponent chooses a permanent they control that shares a card type with the sacrificed
 * permanent and sacrifices it.
 * Draw a card.
 *
 * The sacrificed permanent is read from the cost-payment snapshot ([EffectTarget.SacrificedAsCost]),
 * so its card types are the ones it last had on the battlefield. Each opponent chooses in APNAP
 * order (`forEachPlayerCollecting`), and only then are all the chosen permanents sacrificed
 * together. An opponent with no permanent sharing a card type sacrifices nothing. A copy of the
 * spell uses the original's sacrificed permanent (CR 707.10, 2022-04-29 ruling).
 */
val FatalGrudge = card("Fatal Grudge") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a nonland permanent.\n" +
        "Each opponent chooses a permanent they control that shares a card type with the sacrificed " +
        "permanent and sacrifices it.\n" +
        "Draw a card."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.NonlandPermanent))

    spell {
        effect = Effects.Pipeline {
            val (chosen) = forEachPlayerCollecting(Player.EachOpponent) {
                val candidates = gather(
                    CardSource.ControlledPermanents(
                        Player.You,
                        GameObjectFilter.Permanent.sharingCardTypeWith(EffectTarget.SacrificedAsCost(0))
                    )
                )
                listOf(
                    chooseExactly(
                        1,
                        from = candidates,
                        chooser = Chooser.Controller,
                        prompt = "Choose a permanent that shares a card type with the sacrificed permanent to sacrifice",
                        useTargetingUI = true
                    )
                )
            }
            sacrifice(chosen)
            run(Effects.DrawCards(1))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec7de8d6-edad-4845-9868-781351480258.jpg?1783923087"

        ruling("2022-04-29", "If an effect allows a player to cast Fatal Grudge without paying its mana cost, that player must still pay the additional cost of sacrificing a nonland permanent.")
        ruling("2022-04-29", "If a player copies Fatal Grudge, the characteristics of the permanent that was sacrificed to pay for the original spell are used to determine which permanents each opponent may choose.")
    }
}
