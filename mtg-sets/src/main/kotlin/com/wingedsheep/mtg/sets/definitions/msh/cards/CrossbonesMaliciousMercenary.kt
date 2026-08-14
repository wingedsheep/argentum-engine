package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crossbones, Malicious Mercenary — Marvel Super Heroes #91
 * {3}{B} · Legendary Creature — Human Mercenary Villain · 3/3
 *
 * Deathtouch
 * Whenever another Villain you control enters, put a +1/+1 counter on Crossbones. He deals 2
 * damage to each opponent. This ability triggers only once each turn.
 *
 * Implementation notes:
 * - The Yellowjacket, Heartless Marauder shape: a Villain-*permanent* enters trigger (Villain
 *   shows up on artifact creatures and tokens too) with [TriggerBinding.OTHER] so Crossbones'
 *   own arrival doesn't fire it.
 * - "This ability triggers only once each turn" is the builder's `oncePerTurn` flag — the second
 *   Villain of the turn doesn't put the ability on the stack at all.
 * - "**He** deals 2 damage" — the damage source is Crossbones, which is the ability's source, so
 *   no `damageSource` override is needed. The recipient is [Player.EachOpponent], not a target.
 */
val CrossbonesMaliciousMercenary = card("Crossbones, Malicious Mercenary") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Mercenary Villain"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever another Villain you control enters, put a +1/+1 counter on Crossbones. He " +
        "deals 2 damage to each opponent. This ability triggers only once each turn."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.VILLAIN).youControl(),
            binding = TriggerBinding.OTHER,
        )
        oncePerTurn = true
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
        )
        description = "Whenever another Villain you control enters, put a +1/+1 counter on " +
            "Crossbones. He deals 2 damage to each opponent. This ability triggers only once " +
            "each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Kevin Sidharta"
        flavorText = "\"I consider myself an artist—a craftsman who specializes in murder, " +
            "destruction, and terror!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1576148a-2371-49ba-8eef-0bc2ec3dcaf3.jpg?1783902946"
    }
}
