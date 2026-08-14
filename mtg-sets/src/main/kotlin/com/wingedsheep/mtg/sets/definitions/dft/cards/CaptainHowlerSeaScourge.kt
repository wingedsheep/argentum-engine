package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Captain Howler, Sea Scourge — Aetherdrift #194
 * {2}{U}{R} · Legendary Creature — Shark Pirate · 5/4
 *
 * Ward—{2}, Pay 2 life.
 * Whenever you discard one or more cards, target creature gets +2/+0 until end of turn for
 * each card discarded this way. Whenever that creature deals combat damage to a player this
 * turn, you draw a card.
 *
 * - Ward—{2}, Pay 2 life is one composite ward cost ([WardCost.Composite]); declining either
 *   half counters the targeting spell or ability (CR 702.21a). Same shape as Gisa, the
 *   Hellraiser.
 * - The payoff is batch-worded (CR 603.2c), so it uses [Triggers.YouDiscardOneOrMore]: one
 *   trigger per discard *event*, however many cards it contained. The pump reads the batch
 *   size back through [ContextPropertyKey.TRIGGER_DISCARD_COUNT] and doubles it
 *   ([DynamicAmount.Multiply]) for the printed "+2/+0 ... for each card". Discarding three
 *   cards to one effect gives +6/+0; three sequential single discards fire three separate
 *   triggers for +2/+0 each. The amount is evaluated once, as the ability resolves, and baked
 *   into the layer-7c floating effect.
 * - The second sentence is a delayed triggered ability (CR 603.7a) created by the same
 *   resolution, not an ability granted to the creature: it is scoped to the pumped creature
 *   via `watchedTarget`, expires at end of turn, and — crucially — is controlled by Captain
 *   Howler's controller, so *you* draw even when the target is a creature you don't control.
 *   `fireOnce` stays false because the printed wording is "Whenever", so a double strike
 *   creature connecting twice draws two cards.
 * - "target creature" is unrestricted ([Targets.Creature]) — any creature, not just yours.
 */
val CaptainHowlerSeaScourge = card("Captain Howler, Sea Scourge") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Shark Pirate"
    power = 5
    toughness = 4
    oracleText = "Ward—{2}, Pay 2 life.\n" +
        "Whenever you discard one or more cards, target creature gets +2/+0 until end of turn " +
        "for each card discarded this way. Whenever that creature deals combat damage to a " +
        "player this turn, you draw a card."

    keywordAbility(
        KeywordAbility.wardComposite(WardCost.Mana("{2}"), WardCost.Life(2))
    )

    triggeredAbility {
        trigger = Triggers.YouDiscardOneOrMore
        val creature = target("creature", Targets.Creature)
        effect = Effects.Composite(listOf(
            Effects.ModifyStats(
                power = DynamicAmount.Multiply(
                    DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCARD_COUNT),
                    2
                ),
                toughness = DynamicAmount.Fixed(0),
                target = creature
            ),
            CreateDelayedTriggerEffect(
                effect = Effects.DrawCards(1),
                trigger = Triggers.dealsDamage(
                    damageType = DamageType.Combat,
                    recipient = RecipientFilter.AnyPlayer
                ),
                watchedTarget = creature,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0957c90f-e10d-40f8-a4be-9e9ef623dd43.jpg?1783907861"
    }
}
