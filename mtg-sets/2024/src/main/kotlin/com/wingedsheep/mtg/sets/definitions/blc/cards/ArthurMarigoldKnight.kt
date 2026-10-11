package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arthur, Marigold Knight
 * {2}{U}{R}{W}
 * Legendary Creature — Mouse Knight
 * 4/5
 *
 * Haste
 * Whenever Arthur and at least one other creature attack, look at the top six cards of your
 * library. You may put a creature card from among them onto the battlefield tapped and attacking.
 * Put the rest on the bottom of your library in a random order. Return that creature to its
 * owner's hand at end of combat.
 *
 * - "Arthur and at least one other creature attack" is the self-bound battalion shape
 *   `AttackerCountAtLeast(2)` (Sokka, Lateral Strategist).
 * - The look/choose/rest pipeline is `lookAtTopAndTakeMatching` spelled inline, because the kept
 *   card has to be tracked (`moveTracked`) for the end-of-combat return. "You may" is the
 *   `chooseUpToSplit(1)` selection.
 * - The return is a per-creature delayed trigger at `Step.END_COMBAT` baked onto the creature that
 *   actually entered (Random Encounter's shape), so declining or a card that failed to enter
 *   schedules nothing, and a creature that already left the battlefield is not returned.
 */
val ArthurMarigoldKnight = card("Arthur, Marigold Knight") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Mouse Knight"
    power = 4
    toughness = 5
    oracleText = "Haste\n" +
        "Whenever Arthur and at least one other creature attack, look at the top six cards of your " +
        "library. You may put a creature card from among them onto the battlefield tapped and " +
        "attacking. Put the rest on the bottom of your library in a random order. Return that " +
        "creature to its owner's hand at end of combat."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(2)))
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card onto the battlefield tapped and attacking",
                selectedLabel = "Put onto the battlefield tapped and attacking",
                remainderLabel = "Put on the bottom of your library",
                showAllCards = true
            )
            val entered = moveTracked(
                kept,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking)
            )
            move(rest, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom), order = CardOrder.Random)
            run(
                Effects.ForEachInCollection(
                    collection = entered,
                    effect = Effects.CreateDelayedTrigger(
                        step = Step.END_COMBAT,
                        effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND)
                    )
                )
            )
        }
        description = "Whenever Arthur and at least one other creature attack, look at the top six " +
            "cards of your library. You may put a creature card from among them onto the battlefield " +
            "tapped and attacking. Put the rest on the bottom of your library in a random order. " +
            "Return that creature to its owner's hand at end of combat."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b3603aa-58a2-41d4-a58f-2b3306876e35.jpg?1783910738"

        ruling("2024-07-26", "You choose the player, planeswalker, or battle the creature you put onto the battlefield is attacking. It doesn't have to be the same player, planeswalker, or battle that Arthur or any other attacking creatures are attacking.")
        ruling("2024-07-26", "Although the creature you put onto the battlefield is attacking, it was never declared as an attacking creature. Abilities that trigger whenever a creature attacks won't trigger when that creature enters attacking.")
    }
}
