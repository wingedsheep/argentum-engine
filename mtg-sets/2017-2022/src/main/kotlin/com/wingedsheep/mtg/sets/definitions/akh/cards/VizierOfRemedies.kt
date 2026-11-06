package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Vizier of Remedies
 * {1}{W}
 * Creature — Human Cleric
 * 2/1
 * If one or more -1/-1 counters would be put on a creature you control, that many -1/-1 counters
 * minus one are put on it instead.
 *
 * Hardened Scales' [ModifyCounterPlacement] with a negative modifier, scoped to -1/-1 counters.
 * The line gates only on the recipient ("would be put on"), so `placer` stays `Player.Any` — an
 * opponent's wither damage or effect is reduced too. A harmful counter kind is minimised in the
 * affected player's order (CR 616.1), and a placement reduced to zero puts nothing on. The
 * recipient is not `Recipient.Self`, so the Vizier doesn't reduce counters it enters with itself
 * (per its ruling), while other creatures entering under your control with -1/-1 counters do get
 * one fewer. Each additional Vizier subtracts one more.
 */
val VizierOfRemedies = card("Vizier of Remedies") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "If one or more -1/-1 counters would be put on a creature you control, that many " +
        "-1/-1 counters minus one are put on it instead."

    replacementEffect(
        ModifyCounterPlacement(
            modifier = -1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.MINUS_ONE_MINUS_ONE,
                recipient = Recipient.CreatureYouControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Randy Gallegos"
        flavorText = "\"You'll need all your strength for the trials to come.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36ab760e-93e0-4dbc-aaa1-02316f62ed3f.jpg?1783936529"
        ruling(
            "2017-04-18",
            "\"Put on a creature you control\" includes that creature entering the battlefield with -1/-1 " +
                "counters on it. If a creature would enter the battlefield under your control with a number " +
                "of -1/-1 counters on it while you control Vizier of Remedies, it enters with that many " +
                "counters minus one."
        )
        ruling(
            "2017-04-18",
            "If an effect puts Vizier of Remedies onto the battlefield with one or more -1/-1 counters on " +
                "it, its effect won't apply. This is because you must control Vizier of Remedies before the " +
                "creature begins to enter the battlefield for its effect to apply. The same is true of any " +
                "creatures entering the battlefield with -1/-1 counters on them at the same time as Vizier " +
                "of Remedies enters the battlefield."
        )
        ruling(
            "2017-04-18",
            "If a creature you control is dealt damage by a source with wither or infect, that much damage " +
                "is dealt, but one fewer -1/-1 counter is put on your creature. For example, if a 2/2 " +
                "creature with lifelink and infect is blocked by Vizier of Remedies, the first creature's " +
                "controller gains 2 life and puts one -1/-1 counter on Vizier of Remedies."
        )
        ruling(
            "2017-04-18",
            "If multiple creatures with wither and/or infect deal damage to a creature at once, Vizier of " +
                "Remedies causes only one counter fewer to be put on that creature. Its effect doesn't apply " +
                "separately for each creature dealing damage."
        )
        ruling(
            "2017-04-18",
            "Each additional Vizier of Remedies you control will decrease the number of -1/-1 counters put " +
                "on a creature by one."
        )
    }
}
