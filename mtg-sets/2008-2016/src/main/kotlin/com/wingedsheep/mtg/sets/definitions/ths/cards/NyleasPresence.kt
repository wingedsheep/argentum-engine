package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nylea's Presence — Theros #169 (canonical printing)
 * {1}{G} · Enchantment — Aura
 *
 * Enchant land
 * When this Aura enters, draw a card.
 * Enchanted land is every basic land type in addition to its other types.
 *
 * The land types are Dryad of the Ilysian Grove's five layer-4 [GrantSubtype]s, scoped to the
 * enchanted permanent ([GroupFilter.attachedCreature] is the generic attached-to scope, not
 * creature-only). The engine derives each basic land type's intrinsic mana ability from the
 * projected subtypes; the land keeps its name, supertypes, other subtypes and abilities.
 */
val NyleasPresence = card("Nylea's Presence") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted land is every basic land type in addition to its other types."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    val enchantedLand = GroupFilter.attachedCreature()
    listOf("Plains", "Island", "Swamp", "Mountain", "Forest").forEach { subtype ->
        staticAbility {
            ability = GrantSubtype(subtype, enchantedLand)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e68f1fd4-1a2f-405b-a592-6c4af6214eae.jpg?1783939741"
        ruling("2013-09-15", "The enchanted land will have the land types Plains, Island, Swamp, Mountain, and Forest. It will also have the mana ability of each basic land type (for example, Forests can tap to produce {G}). It still has its other subtypes and abilities.")
        ruling("2013-09-15", "Giving a land extra basic land types doesn't change its name or whether it's legendary or basic.")
        ruling("2013-09-15", "If the target of an Aura is illegal when it tries to resolve, the Aura won't resolve. The Aura doesn't enter the battlefield, so you won't get to draw a card.")
    }
}
