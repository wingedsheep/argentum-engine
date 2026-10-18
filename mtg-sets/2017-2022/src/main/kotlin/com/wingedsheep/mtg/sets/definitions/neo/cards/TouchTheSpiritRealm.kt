package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Touch the Spirit Realm — Kamigawa: Neon Dynasty #40
 * {2}{W} · Enchantment
 *
 * When this enchantment enters, exile up to one target artifact or creature until this
 * enchantment leaves the battlefield.
 * Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it to the
 * battlefield under its owner's control at the beginning of the next end step.
 *
 * The ETB half is Prayer of Binding's modern O-Ring shape: [Effects.ExileUntilLeaves] (which does
 * nothing if the enchantment has already left — the card's ruling) paired with a leaves trigger
 * returning the linked exile. The channel half is Guardian of Ghirapur's end-step flicker,
 * activated from hand with the discard as part of the cost.
 */
val TouchTheSpiritRealm = card("Touch the Spirit Realm") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile up to one target artifact or creature until this " +
        "enchantment leaves the battlefield.\n" +
        "Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it to the " +
        "battlefield under its owner's control at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CreatureOrArtifact, optional = true)
        effect = Effects.ExileUntilLeaves(exiled)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    // Channel — {1}{W}, Discard this card (from hand): flicker an artifact or creature until the next end step.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val permanent = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.Exile(permanent) then
            Effects.CreateDelayedTrigger(step = Step.END, effect = Effects.Move(permanent, Zone.BATTLEFIELD))
        description = "Channel — {1}{W}, Discard this card: Exile target artifact or creature. Return it " +
            "to the battlefield under its owner's control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e16ab44e-4257-4c0c-b705-8ac1e9c1d835.jpg?1783923911"
        ruling("2022-02-18", "If Touch the Spirit Realm leaves the battlefield before its first ability resolves, the target permanent won't be exiled.")
        ruling("2022-02-18", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
