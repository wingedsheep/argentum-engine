package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angel of Condemnation
 * {2}{W}{W}
 * Creature — Angel
 * 3/3
 * Flying, vigilance
 * {2}{W}, {T}: Exile another target creature. Return that card to the battlefield under its
 * owner's control at the beginning of the next end step.
 * {2}{W}, {T}, Exert this creature: Exile another target creature until this creature leaves
 * the battlefield.
 *
 * The first ability is Galepowder Mage's blink: the delayed trigger returns the card even if the
 * Angel has left by then. The second is [Effects.MoveUntilSourceLeaves]: nothing is exiled if the
 * Angel left before it resolves, and the card comes back the moment the Angel leaves, with no gap
 * for state-based actions.
 */
val AngelOfCondemnation = card("Angel of Condemnation") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "{2}{W}, {T}: Exile another target creature. Return that card to the battlefield under its " +
        "owner's control at the beginning of the next end step.\n" +
        "{2}{W}, {T}, Exert this creature: Exile another target creature until this creature leaves " +
        "the battlefield. (An exerted creature won't untap during your next untap step.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Exile(creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(creature, Zone.BATTLEFIELD)
            )
        description = "Exile another target creature. Return it to the battlefield under its " +
            "owner's control at the beginning of the next end step."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap, Costs.Exert)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.MoveUntilSourceLeaves(creature, Zone.EXILE)
        description = "Exert: Exile another target creature until Angel of Condemnation leaves the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90dad070-e41d-419b-ae27-ace82b9ab2c5.jpg?1783936067"
        ruling("2017-07-14", "If Angel of Condemnation leaves the battlefield before its last ability resolves, the target creature won't be exiled.")
        ruling("2017-07-14", "The card exiled with Angel of Condemnation's last ability returns to the battlefield immediately after Angel of Condemnation leaves the battlefield. Nothing happens between the two events, including state-based actions.")
        ruling("2017-07-14", "Tapping Angel of Condemnation to activate either of its abilities while it's attacking doesn't remove it from combat.")
        ruling("2017-07-14", "If a creature token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2017-07-14", "If Angel of Condemnation leaves the battlefield before its first activated ability resolves, the target creature is still exiled. That card returns to the battlefield even if Angel of Condemnation has left the battlefield before the next end step.")
    }
}
