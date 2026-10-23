package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wake to Slaughter
 * {3}{B}{R}
 * Sorcery
 * Choose up to two target creature cards in your graveyard. An opponent chooses one of them.
 * Return that card to your hand. Return the other to the battlefield under your control. It gains
 * haste. Exile it at the beginning of the next end step.
 * Flashback {4}{B}{R}
 *
 * The still-legal targets are gathered and an opponent ([Chooser.Opponent]) picks exactly one of
 * them for your hand; the remainder is reanimated with haste and a delayed end-step exile. Per the
 * ruling, a lone legal target (or a single chosen target) is auto-selected — it goes to your hand,
 * never the battlefield.
 */
val WakeToSlaughter = card("Wake to Slaughter") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Choose up to two target creature cards in your graveyard. An opponent chooses one of them. " +
        "Return that card to your hand. Return the other to the battlefield under your control. It gains haste. " +
        "Exile it at the beginning of the next end step.\n" +
        "Flashback {4}{B}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)

        effect = Effects.Pipeline {
            val chosenCards = gather(CardSource.ChosenTargets)
            ifNotEmpty(chosenCards) {
                // "An opponent chooses one of them."
                val picked = chooseExactlySplit(
                    count = 1,
                    from = chosenCards,
                    chooser = Chooser.Opponent,
                    prompt = "Choose a card to return to your opponent's hand. The other returns to the " +
                        "battlefield with haste and is exiled at the beginning of the next end step.",
                    selectedLabel = "To hand",
                    remainderLabel = "To battlefield"
                )
                // "Return that card to your hand."
                toHand(picked.selected)
                // "Return the other to the battlefield under your control."
                val reanimated = moveTracked(
                    picked.remainder,
                    CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                )
                ifNotEmpty(reanimated) {
                    // "It gains haste. Exile it at the beginning of the next end step."
                    run(Effects.GrantKeyword(Keyword.HASTE, reanimated.asTarget, Duration.Permanent))
                    run(Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Move(reanimated.asTarget, Zone.EXILE)
                    ))
                }
            }
        }
    }

    keywordAbility(KeywordAbility.flashback("{4}{B}{R}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f02a544-716c-4f09-8ae9-dbfe7ef136d7.jpg?1783925551"
        ruling(
            "2021-09-24",
            "If only one target is still legal as Wake to Slaughter resolves (or if you only chose one target), " +
                "that's the card your opponent will have to choose to put into your hand. They can't choose to " +
                "have you return it to the battlefield."
        )
    }
}
