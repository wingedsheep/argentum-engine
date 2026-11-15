package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Encase in Ice
 * {1}{U}
 * Enchantment — Aura
 * Flash
 * Enchant red or green creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * The enchant restriction is the colour-filtered `auraTarget`; the state-based check re-reads it
 * against the projected host, so the Aura falls off a creature that stops being red or green.
 */
val EncaseInIce = card("Encase in Ice") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Enchant red or green creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Creature.withAnyColor(Color.RED, Color.GREEN)))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcad5bd9-3236-48d4-920f-c3ed12d5f329.jpg?1783938608"
        ruling("2015-02-25", "If the enchanted creature stops being red or green, or if it stops being a creature, Encase in Ice will be put into its owner's graveyard.")
        ruling("2015-02-25", "The creature can still be untapped by other spells and abilities.")
    }
}
