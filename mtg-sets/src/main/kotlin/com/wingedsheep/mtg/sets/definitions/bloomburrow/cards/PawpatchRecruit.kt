package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Pawpatch Recruit
 * {G}
 * Creature — Rabbit Warrior
 * 2/1
 *
 * Offspring {2}
 * Trample
 * Whenever a creature you control becomes the target of a spell or ability an
 * opponent controls, put a +1/+1 counter on target creature you control other
 * than that creature.
 */
val PawpatchRecruit = card("Pawpatch Recruit") {
    manaCost = "{G}"
    typeLine = "Creature — Rabbit Warrior"
    power = 2
    toughness = 1
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\n" +
        "Trample\n" +
        "Whenever a creature you control becomes the target of a spell or ability an opponent controls, put a +1/+1 counter on target creature you control other than that creature."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    keywords(Keyword.TRAMPLE)

    // Whenever a creature you control becomes the target of an opponent's spell/ability,
    // put a +1/+1 counter on target creature you control other than that creature.
    // Note: "other than that creature" approximated with other() (excludes source)
    triggeredAbility {
        trigger = Triggers.CreatureYouControlBecomesTargetByOpponent()
        val creature = target(
            "target creature you control other than that creature",
            TargetCreature(filter = TargetFilter.CreatureYouControl.other())
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4d88ba-0ee4-4f66-995b-2e50614f50ee.jpg?1721426891"
    }
}
