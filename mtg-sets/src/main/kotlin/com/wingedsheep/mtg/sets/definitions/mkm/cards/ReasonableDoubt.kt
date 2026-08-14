package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Reasonable Doubt — Murders at Karlov Manor #69
 * {1}{U} · Instant
 *
 * Counter target spell unless its controller pays {2}.
 * Suspect up to one target creature.
 *
 * Two independent targets, and the spell target must come **first**: `CounterEffectExecutor`
 * reads the countered spell off the head of the target list. The creature is `optional = true`
 * and therefore last, matching the cast-time slot ordering rule.
 *
 * The two sentences are independent — if the targeted spell has left the stack by resolution
 * but the creature is still legal, the spell still resolves and suspects it (CR 608.2b), which
 * is exactly what `Effects.Composite` does here: a failed sub-effect is skipped, not fatal.
 * The reverse also holds — declining the creature (or it dying in response) never stops the
 * counter. There is no "cast it just to suspect" mode: the spell target is mandatory, so a
 * legal spell on the stack is required to cast this at all.
 */
val ReasonableDoubt = card("Reasonable Doubt") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2}.\n" +
        "Suspect up to one target creature. (A suspected creature has menace and can't block.)"

    spell {
        target("target spell", Targets.Spell)
        val creature = target("up to one target creature", TargetCreature(count = 1, optional = true))
        effect = Effects.Composite(
            Effects.CounterUnlessDynamicPays(DynamicAmount.Fixed(2)),
            Effects.Suspect(creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Betty Jiang"
        flavorText = "\"Merle had the motive, but Blovinac was the one caught with the murder " +
            "weapon. And who was the seven-foot-tall hooded figure? This case is a mess!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/7/270570a3-8637-4e4e-92d9-e985474cd5d2.jpg?1783912905"
        ruling("2024-02-02", "You can't cast Reasonable Doubt just to suspect a creature. There has to be a spell on the stack that's a legal target for Reasonable Doubt.")
        ruling("2024-02-02", "When an effect suspects a creature, it becomes suspected. It gains menace and \"This creature can't block\" for as long as it's suspected. It stays suspected until it leaves the battlefield or another effect causes it to no longer be suspected.")
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
    }
}
