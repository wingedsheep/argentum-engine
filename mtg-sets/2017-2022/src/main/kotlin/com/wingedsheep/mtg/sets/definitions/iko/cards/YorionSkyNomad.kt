package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Yorion, Sky Nomad — Ikoria: Lair of Behemoths #232
 * {3}{W/U}{W/U} · Legendary Creature — Bird Serpent · 4/5
 *
 * Companion — the starting deck holds at least twenty cards more than the format's minimum deck
 * size (CR 702.139), so Yorion can never be a Commander companion.
 *
 * The enters trigger chooses as it resolves among the other nonland permanents you both own *and*
 * control (ruling), exiles them, and the end-step trigger carries the cards actually exiled — a
 * token ceases to exist in exile and so never returns. "Those cards" return under their owner's
 * control, which is you.
 */
val YorionSkyNomad = card("Yorion, Sky Nomad") {
    manaCost = "{3}{W/U}{W/U}"
    typeLine = "Legendary Creature — Bird Serpent"
    power = 4
    toughness = 5
    oracleText = "Companion — Your starting deck contains at least twenty cards more than the minimum deck " +
        "size. (If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "Flying\n" +
        "When Yorion enters, exile any number of other nonland permanents you own and control. Return those " +
        "cards to the battlefield at the beginning of the next end step."

    keywordAbility(KeywordAbility.companion(CompanionCondition.DeckSizeAboveMinimum(20)))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val candidates = gather(
                filter = GameObjectFilter.NonlandPermanent.withControllerPredicate(
                    ControllerPredicate.And(listOf(ControllerPredicate.OwnedByYou, ControllerPredicate.ControlledByYou))
                ),
                excludeSelf = true,
            )
            val chosen = chooseAnyNumber(
                from = candidates,
                useTargetingUI = true,
                prompt = "Exile any number of other nonland permanents you own and control " +
                    "(they return at the beginning of the next end step)",
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE), name = "yorionExiled")
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
                },
                carryCollections = listOf(exiled.key),
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/275426c4-c14e-47d0-a9d4-24da7f6f6911.jpg?1783931007"
        ruling("2020-04-17", "Your minimum deck size is forty cards for Limited events (such as Booster Draft and Sealed Deck) and sixty cards for Constructed events (such as Standard or casual freeform play). Certain variants may have other minimums. The Commander variant requires exactly one hundred cards, so Yorion can never be your chosen companion in a Commander game.")
        ruling("2020-04-17", "You choose which permanents to exile as Yorion's triggered ability resolves. No player may take action between the time you choose the permanents and the time they're exiled.")
        ruling("2020-04-17", "You can't exile permanents you control but don't own, or permanents that you own but don't control.")
        ruling("2020-04-17", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
