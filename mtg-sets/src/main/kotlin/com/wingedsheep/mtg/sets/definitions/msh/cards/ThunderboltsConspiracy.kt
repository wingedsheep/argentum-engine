package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunderbolts Conspiracy — Marvel Super Heroes #117
 * {3}{B} · Enchantment · Rare
 *
 * Flash
 * Whenever a Villain you control dies, return it to the battlefield under its owner's control with a
 * finality counter on it. That creature is a Hero in addition to its other types.
 *
 * The Valkyrie's Call template: a filtered dies trigger ([Triggers.leavesBattlefield] to
 * [Zone.GRAVEYARD], [TriggerBinding.ANY]) whose filter is "Villain creature you control".
 * [EffectTarget.TriggeringEntity] resolves to the dying card, now in the graveyard;
 * [Effects.Move] returns it to the battlefield under its owner's control (the reanimation default),
 * then the [Counters.FINALITY] counter and the [Duration.Permanent] Hero type-add apply to the new
 * object created by that return (CR 611.2b — the effect has no duration, so it modifies the object
 * for as long as it stays on the battlefield).
 *
 * The finality counter's "if it would die, exile it instead" replacement is engine-intrinsic to
 * [Counters.FINALITY], so the loop self-terminates on the creature's second death. A token Villain
 * dying leaves nothing to return — the Move is a graceful no-op, matching the rules.
 */
val ThunderboltsConspiracy = card("Thunderbolts Conspiracy") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "Whenever a Villain you control dies, return it to the battlefield under its owner's " +
        "control with a finality counter on it. That creature is a Hero in addition to its other " +
        "types. (If a creature with a finality counter on it would die, exile it instead.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.VILLAIN).youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.Composite(
            // Return it to the battlefield under its owner's control ...
            Effects.Move(EffectTarget.TriggeringEntity, Zone.BATTLEFIELD),
            // ... with a finality counter on it.
            Effects.AddCounters(Counters.FINALITY, 1, EffectTarget.TriggeringEntity),
            // That creature is a Hero in addition to its other types.
            Effects.AddCreatureType("Hero", EffectTarget.TriggeringEntity, Duration.Permanent)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Lucio Parrillo"
        flavorText = "Justice, like lightning."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f498c1a4-54d2-4e87-952f-8cf7e408930c.jpg?1783902936"
    }
}
