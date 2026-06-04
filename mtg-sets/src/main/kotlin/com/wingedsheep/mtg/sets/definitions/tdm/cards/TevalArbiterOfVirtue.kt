package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Teval, Arbiter of Virtue — Tarkir: Dragonstorm #230
 * {2}{B}{G}{U} · Legendary Creature — Spirit Dragon · Mythic
 * 6/6
 *
 * Flying, lifelink
 * Spells you cast have delve. (Each card you exile from your graveyard while casting those spells
 * pays for {1}.)
 * Whenever you cast a spell, you lose life equal to its mana value.
 *
 * "Spells you cast have delve" is a runtime keyword grant: [GrantKeywordToOwnSpells] adds DELVE to
 * the caster's spells, consulted by every delve read site (cast enumerator, cost utils, payment
 * handler) via the shared granted-keyword resolver. The lose-life rider reads the cast spell's
 * mana value from the triggering entity (still on the stack while Teval's trigger resolves above it).
 */
val TevalArbiterOfVirtue = card("Teval, Arbiter of Virtue") {
    manaCost = "{2}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying, lifelink\n" +
        "Spells you cast have delve. (Each card you exile from your graveyard while casting those " +
        "spells pays for {1}.)\n" +
        "Whenever you cast a spell, you lose life equal to its mana value."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.DELVE,
            spellFilter = GameObjectFilter.Any
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        effect = Effects.LoseLife(
            DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue),
            EffectTarget.Controller
        )
        description = "Whenever you cast a spell, you lose life equal to its mana value."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27a93f5b-7b32-49f0-a179-b897828fe49a.jpg?1743204911"
    }
}
