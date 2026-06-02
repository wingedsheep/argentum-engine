package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonback Assault
 * {3}{G}{U}{R}
 * Enchantment
 *
 * When this enchantment enters, it deals 3 damage to each creature and each planeswalker.
 * Landfall — Whenever a land you control enters, create a 4/4 red Dragon creature token
 * with flying.
 *
 * The board-wipe ETB iterates the single combined group [GameObjectFilter.CreatureOrPlaneswalker]
 * (the same primitive Goblin Chainwhirler-style sweeps use), dealing 3 damage from the
 * enchantment to each member. The landfall half reuses [Triggers.LandYouControlEnters] +
 * [Effects.CreateToken], mirroring Rampaging Baloths.
 */
val DragonbackAssault = card("Dragonback Assault") {
    manaCost = "{3}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, it deals 3 damage to each creature and each planeswalker.\n" +
        "Landfall — Whenever a land you control enters, create a 4/4 red Dragon creature token with flying."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
            DealDamageEffect(3, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/3/434c2965-82b8-4e89-bf45-a8fc093f9a21.jpg?1743176601",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "179"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d54cc838-d79d-433a-99fb-d6e4d1c1431d.jpg?1743204697"
        ruling("2025-04-04", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2025-04-04", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
    }
}
