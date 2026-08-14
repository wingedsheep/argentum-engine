package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Extraordinary Journey
 * {X}{X}{U}{U}
 * Enchantment
 * When this enchantment enters, exile up to X target creatures. For each of those cards, its owner
 * may play it for as long as it remains exiled.
 * Whenever one or more nontoken creatures enter, if one or more of them entered from exile or was
 * cast from exile, you draw a card. This ability triggers only once each turn.
 *
 * **The ETB.** X is read as [DynamicAmount.CastX], not `XValue`: by the time the enters-trigger goes
 * on the stack the spell is gone, so the cap must come from the durable cast-choices bag that rode
 * onto the permanent (the same reading The Goose Mother and Ingenious Prodigy use). `optional = true`
 * makes X = 0 — and a board with fewer than X creatures — resolve cleanly instead of fizzling. The
 * exile-plus-permission half is the Suspend Aggression pipeline with `ownerControls = true` (the
 * exiled creatures may belong to any player, and the printed clause hands the permission to each
 * card's *owner*) and `MayPlayExpiry.Permanent` for "**for as long as it remains exiled**".
 *
 * **The draw trigger** watches *every* player's nontoken creatures — the second ruling is explicit
 * that it is not limited to the creatures this enchantment exiled — hence `anyController()` rather
 * than the batch trigger's you-control default.
 *
 * "if one or more of them …" is a real intervening-"if" ([Conditions.AnyEnteredOrWasCastFromExile])
 * rather than a resolution-time gate, and that distinction is load-bearing here: the ability is also
 * `oncePerTurn`, and CR 603.4 says an ability whose intervening-"if" is false never triggers at all.
 * A batch of creatures arriving from hand must therefore leave the turn's single firing unspent.
 */
val ExtraordinaryJourney = card("Extraordinary Journey") {
    manaCost = "{X}{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile up to X target creatures. For each of those " +
        "cards, its owner may play it for as long as it remains exiled.\n" +
        "Whenever one or more nontoken creatures enter, if one or more of them entered from exile " +
        "or was cast from exile, you draw a card. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetPermanent(
            optional = true,
            filter = TargetFilter.Creature,
            dynamicMaxCount = DynamicAmount.CastX
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.ChosenTargets,
                storeAs = "extraordinaryJourney_exiled"
            ),
            MoveCollectionEffect(
                from = "extraordinaryJourney_exiled",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect(
                from = "extraordinaryJourney_exiled",
                expiry = MayPlayExpiry.Permanent,
                ownerControls = true
            )
        )
        description = "When this enchantment enters, exile up to X target creatures. For each of " +
            "those cards, its owner may play it for as long as it remains exiled."
    }

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(
            GameObjectFilter.Creature.nontoken().anyController()
        )
        triggerCondition = Conditions.AnyEnteredOrWasCastFromExile
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "Whenever one or more nontoken creatures enter, if one or more of them " +
            "entered from exile or was cast from exile, you draw a card. This ability triggers " +
            "only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a69fb480-a9fc-4f09-ac4e-3ce52c485ea9.jpg?1783915121"

        ruling(
            "2023-09-01",
            "Players must pay all costs and follow all normal timing rules for spells cast with " +
                "Extraordinary Journey's first ability."
        )
        ruling(
            "2023-09-01",
            "Extraordinary Journey's second ability applies to all creatures that enter the " +
                "battlefield from exile or were cast from exile, not just creatures cast with " +
                "Extraordinary Journey's first ability."
        )
    }
}
