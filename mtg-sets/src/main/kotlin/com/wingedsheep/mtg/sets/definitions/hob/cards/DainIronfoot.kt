package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dáin Ironfoot
 * {2}{R}
 * Legendary Creature — Dwarf Warrior
 * 1/4
 * When Dáin enters, create a colorless Equipment artifact token named Axe with "Equipped creature
 * gets +1/+0" and equip {2}. When you do, attach it to target creature you control.
 * Whenever Dáin attacks, each equipped attacking creature gains double strike until end of turn.
 *
 *  - The Axe is the named Equipment token already registered in `PredefinedTokens.kt` (shared with
 *    Iron Hills Blacksmith), minted by [CreatePredefinedTokenEffect] so its static bonus and equip
 *    ability are real printed abilities rather than an ad-hoc grant.
 *  - "When you do" is a genuine **reflexive trigger** (CR 603.12), not a second clause of the same
 *    ability: the attach goes on the stack separately and picks its target then, so opponents get a
 *    priority window between the token appearing and it being attached. [ReflexiveTriggerEffect]
 *    with `optional = false` — creating the token isn't a "may", so the reflexive half always fires.
 *    The freshly minted token is addressed through the [CREATED_TOKENS] pipeline collection, which
 *    the reflexive trigger carries over from its action half.
 *  - The attach is not an equip activation: no cost is paid and it happens at instant speed.
 *  - "Each **equipped attacking** creature" is deliberately not controller-scoped — the printed text
 *    says neither "you control" nor "attacking creature you control". In practice only the attacking
 *    player has attackers, but the filter mirrors the wording rather than the common case. Dáin
 *    itself is included when it is carrying an Equipment.
 */
val DainIronfoot = card("Dáin Ironfoot") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Warrior"
    power = 1
    toughness = 4
    oracleText = "When Dáin enters, create a colorless Equipment artifact token named Axe with " +
        "\"Equipped creature gets +1/+0\" and equip {2}. When you do, attach it to target creature " +
        "you control.\n" +
        "Whenever Dáin attacks, each equipped attacking creature gains double strike until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = CreatePredefinedTokenEffect("Axe"),
            optional = false,
            reflexiveEffect = Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                creatureTarget = EffectTarget.ContextTarget(0),
            ),
            reflexiveTargetRequirements = listOf(Targets.CreatureYouControl),
            descriptionOverride = "Create a colorless Equipment artifact token named Axe with " +
                "\"Equipped creature gets +1/+0\" and equip {2}. When you do, attach it to target " +
                "creature you control.",
        )
        description = "When Dáin enters, create a colorless Equipment artifact token named Axe " +
            "with \"Equipped creature gets +1/+0\" and equip {2}. When you do, attach it to " +
            "target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.DOUBLE_STRIKE,
            GroupFilter(GameObjectFilter.Creature.attacking().equipped())
        )
        description = "Whenever Dáin attacks, each equipped attacking creature gains double " +
            "strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7112e460-9160-4535-ad94-93f1f4ac04cf.jpg?1785236701"
    }
}
