package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** "a card with the same name as one of the exiled cards" — read pile-wide off this enchantment. */
private val SEARCH_THE_CITY_NAMED_IN_PILE: GameObjectFilter =
    GameObjectFilter.Any.sharingNameWithLinkedExile()

private const val SEARCH_THE_CITY_PLAY_TEXT =
    "Whenever you play a card with the same name as one of the exiled cards, you may put one of " +
        "those cards with that name into its owner's hand. Then if there are no cards exiled with " +
        "this enchantment, sacrifice it. If you do, take an extra turn after this one."

/**
 * The shared resolution of both "play" triggers: optionally return one exiled card sharing the
 * played card's name, then — whether or not one was returned — check the pile and, if it is
 * empty, sacrifice for an extra turn.
 */
private val SEARCH_THE_CITY_PAYOFF: Effect = Effects.Pipeline {
    val pile = gather(CardSource.FromLinkedExile())
    val found = chooseUpTo(
        1,
        from = pile,
        filter = GameObjectFilter.Any.sharingNameWith(EffectTarget.TriggeringEntity),
        prompt = "You may put an exiled card with the same name into its owner's hand"
    )
    toHand(found)
    ifThen(
        Conditions.CompareAmounts(DynamicAmounts.linkedExileCardCount(), ComparisonOperator.EQ, 0)
    ) {
        run(
            Effects.IfYouDo(
                action = Effects.SacrificeTarget(EffectTarget.Self),
                then = Effects.TakeExtraTurn(),
                successCriterion = SuccessCriterion.PermanentsSacrificed,
                descriptionOverride = "Sacrifice this enchantment. If you do, take an extra turn " +
                    "after this one."
            )
        )
    }
}

/**
 * Search the City — Return to Ravnica #49
 * {4}{U} · Enchantment · Rare
 *
 * When this enchantment enters, exile the top five cards of your library.
 * Whenever you play a card with the same name as one of the exiled cards, you may put one of
 * those cards with that name into its owner's hand. Then if there are no cards exiled with this
 * enchantment, sacrifice it. If you do, take an extra turn after this one.
 *
 * Modelling notes:
 * - The five cards are a **linked** exile (CR 607), so "the exiled cards" is this enchantment's own
 *   pile and is re-read live: a card returned to hand drops out of it.
 * - **"Play" is casting a spell or playing a land**, two different events, so the printed ability
 *   is authored as two triggered abilities with one shared resolution. They can never both fire
 *   for the same play, so together they behave exactly as the single printed ability.
 *     - The cast side filters the spell with `sharingNameWithLinkedExile()` on the trigger itself.
 *     - The land side has no spell filter, so the same name check is a `triggerRestriction` over
 *       the land just played — checked once, when the land is played, never again (the trigger is
 *       not an intervening "if").
 * - "Then if there are no cards exiled" is checked on resolution whether or not a card was
 *   returned — so if the matching card left exile in response, the trigger still resolves and an
 *   empty pile still sacrifices the enchantment for the extra turn.
 * - The extra turn is gated on the sacrifice actually happening ("If you do").
 */
val SearchTheCity = card("Search the City") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile the top five cards of your library.\n" +
        SEARCH_THE_CITY_PLAY_TEXT

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val topFive = gather(CardSource.TopOfLibrary(5))
            exile(topFive, linkToSource = true)
        }
        description = "When this enchantment enters, exile the top five cards of your library."
    }

    // "play" — casting a spell with the same name as one of the exiled cards.
    triggeredAbility {
        trigger = Triggers.you.casts(SEARCH_THE_CITY_NAMED_IN_PILE)
        effect = SEARCH_THE_CITY_PAYOFF
        description = SEARCH_THE_CITY_PLAY_TEXT
    }

    // "play" — playing a land with the same name as one of the exiled cards.
    triggeredAbility {
        trigger = Triggers.you.playsLand()
        triggerRestriction = Conditions.EntityMatches(
            EffectTarget.TriggeringEntity,
            SEARCH_THE_CITY_NAMED_IN_PILE
        )
        effect = SEARCH_THE_CITY_PAYOFF
        description = SEARCH_THE_CITY_PLAY_TEXT
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cd77575-fedc-45b1-a53b-dfed0f34c875.jpg?1783940366"

        ruling(
            "2012-10-01",
            "To “play a card” is to play that card as a land or cast that card as a spell, " +
                "whichever is appropriate."
        )
        ruling("2012-10-01", "You can put only one card into its owner’s hand for each card you play.")
        ruling(
            "2012-10-01",
            "If you don’t sacrifice Search the City, perhaps because it was destroyed in response to " +
                "its ability triggering, you won’t take an extra turn."
        )
    }
}
