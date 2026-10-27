package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bubble Snare — Zendikar Rising #47
 * {U} · Enchantment — Aura
 *
 * Kicker {2}{U}
 * Enchant creature
 * When this Aura enters, if it was kicked, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * The tap is an intervening-if ETB trigger on [WasKicked]; the lock is a static
 * DOESNT_UNTAP grant to the enchanted creature.
 */
val BubbleSnare = card("Bubble Snare") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\n" +
        "Enchant creature\n" +
        "When this Aura enters, if it was kicked, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bfba7a0-d5ca-4197-9e58-21f28345f1a6.jpg"
    }
}
