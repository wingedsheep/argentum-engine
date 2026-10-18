package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Clawing Torment — Kamigawa: Neon Dynasty #91
 * {B} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets -1/-1 and can't block.
 * Enchanted permanent has "At the beginning of your upkeep, you lose 1 life."
 *
 * Short Circuit's shape for the creature-gated half ([Conditions.EnchantedPermanentMatches] on the
 * host's projected type), and Farmstead / Relic Bane's granted upkeep trigger, so "you" is the
 * enchanted permanent's controller, not the Aura's.
 */
val ClawingTorment = card("Clawing Torment") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets -1/-1 and can't block.\n" +
        "Enchanted permanent has \"At the beginning of your upkeep, you lose 1 life.\""

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(-1, -1, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)),
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/621fce96-5933-4e2b-98ec-2589940e24cb.jpg?1783923889"
    }
}
