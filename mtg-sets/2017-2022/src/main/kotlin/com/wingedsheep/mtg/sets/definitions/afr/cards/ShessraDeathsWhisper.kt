package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shessra, Death's Whisper
 * {2}{B}{G}
 * Legendary Creature — Human Elf Warlock
 * 1/3
 *
 * Bewitching Whispers — When Shessra enters, target creature blocks this turn if able.
 * Whispers of the Grave — At the beginning of your end step, if a creature died this turn, you may
 * pay 2 life. If you do, draw a card.
 *
 * The end-step clause is an intervening if ([Conditions.CreatureDiedThisTurn]), checked when the
 * trigger would fire and again on resolution — so it triggers at most once, and only if a creature
 * had already died as the end step began.
 */
val ShessraDeathsWhisper = card("Shessra, Death's Whisper") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Elf Warlock"
    power = 1
    toughness = 3
    oracleText = "Bewitching Whispers — When Shessra enters, target creature blocks this turn if able.\n" +
        "Whispers of the Grave — At the beginning of your end step, if a creature died this turn, " +
        "you may pay 2 life. If you do, draw a card."

    triggeredAbility {
        val creature = target(TargetFilter.Creature)
        trigger = Triggers.self.enters()
        effect = Effects.MarkMustBlockThisTurn(target = creature)
        description = "When Shessra enters, target creature blocks this turn if able."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Marie Magny"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60ca5fef-af21-4e2b-9efa-61e08a638a1f.jpg?1783926444"

        ruling(
            "2021-07-23",
            "If the creature affected by the Bewitching Whispers ability is tapped or is affected by " +
                "a spell or ability that says it can't block, then it doesn't block. If there's a cost " +
                "associated with having that creature block, its controller isn't forced to pay that " +
                "cost, so it doesn't have to block in that case either."
        )
        ruling(
            "2021-07-23",
            "Shessra, Death's Whisper's last ability will trigger only once during your end step, no " +
                "matter how many creatures died this turn. However, if no creatures have died so far " +
                "this turn as the end step begins, the ability won't trigger at all. It's not possible " +
                "to cause a creature to die during the end step in time to have the ability trigger."
        )
    }
}
