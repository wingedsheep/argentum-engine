package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aether Meltdown — Kaladesh #36
 * {1}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant creature or Vehicle
 * When this Aura enters, you get {E}{E} (two energy counters).
 * Enchanted creature gets -4/-0.
 *
 * The enchant clause is [GameObjectFilter.CreatureOrVehicle], as on Aerial Modification. The -4/-0
 * only applies while the enchanted permanent is a creature — an uncrewed Vehicle is unaffected until
 * it's crewed.
 */
val AetherMeltdown = card("Aether Meltdown") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Enchant creature or Vehicle\n" +
        "When this Aura enters, you get {E}{E} (two energy counters).\n" +
        "Enchanted creature gets -4/-0."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    staticAbility {
        ability = ModifyStats(-4, 0)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91f99614-45e2-4688-8403-f3a6b9162b08.jpg?1783937224"
        ruling(
            "2016-09-20",
            "If the target creature or Vehicle becomes an illegal target while Aether Meltdown is on the " +
                "stack, Aether Meltdown won't resolve. It won't enter the battlefield and you won't get {E}{E}."
        )
    }
}
