package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Paladin Elizabeth Taggerdy
 * {1}{R}{W}
 * Legendary Creature — Human Knight
 * 3/2
 *
 * Battalion — Whenever Paladin Elizabeth Taggerdy and at least two other creatures attack, draw a
 * card, then you may put a creature card with mana value X or less from your hand onto the
 * battlefield tapped and attacking, where X is Paladin Elizabeth Taggerdy's power.
 *
 * Battalion is the self attack trigger with `AttackerCountAtLeast(3)` (Aurelia, the Law Above).
 * The draw runs first inside the pipeline, so the card just drawn is eligible; the hand is then
 * gathered and narrowed by `manaValueAtMostDynamic(powerOf(Self))` — X is read on resolution, and
 * `Self` falls back to last-known power if Elizabeth has left the battlefield (ruling). The "may"
 * is `chooseUpTo(1)`, and the put is Kinscaer Sentry's `ZonePlacement.TappedAndAttacking`, which
 * never counts as declaring an attacker.
 */
val PaladinElizabethTaggerdy = card("Paladin Elizabeth Taggerdy") {
    manaCost = "{1}{R}{W}"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = "Battalion — Whenever Paladin Elizabeth Taggerdy and at least two other creatures attack, " +
        "draw a card, then you may put a creature card with mana value X or less from your hand onto the " +
        "battlefield tapped and attacking, where X is Paladin Elizabeth Taggerdy's power."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val handCreatures = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
            val eligible = filter(
                handCreatures,
                GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.powerOf(EffectTarget.Self))
            )
            val chosen = chooseUpTo(
                1,
                from = eligible,
                prompt = "You may put a creature card onto the battlefield tapped and attacking",
                selectedLabel = "Put onto the battlefield tapped and attacking"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Wisnu Tan"
        flavorText = "\"Into the killing ground!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a24fd14b-cfd1-43db-b155-3187a1063d92.jpg?1783912385"
        ruling("2024-03-08", "The three attacking creatures don’t have to be attacking the same player, planeswalker, or battle.")
        ruling("2024-03-08", "Once Paladin Elizabeth Taggerdy’s battalion ability has triggered, it doesn’t matter how many creatures are still attacking when that ability resolves.")
        ruling("2024-03-08", "If Paladin Elizabeth Taggerdy is no longer on the battlefield when its battalion ability resolves, use its power as it last existed on the battlefield to determine the value of X.")
        ruling("2024-03-08", "Although the creature you put onto the battlefield is attacking, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won’t trigger when it enters the battlefield attacking.")
        ruling("2024-03-08", "You choose which player, planeswalker, or battle the creature you put onto the battlefield is attacking as it enters the battlefield. It doesn’t have to be attacking the same player, planeswalker, or battle that Paladin Elizabeth Taggerdy is attacking.")
    }
}
