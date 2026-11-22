package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Artificer's Hex
 * {B}
 * Enchantment — Aura
 * Enchant Equipment
 * At the beginning of your upkeep, if enchanted Equipment is attached to a creature, destroy that
 * creature.
 *
 * "That creature" sits two attachments away from the Aura: the creature that has attached to it
 * the Equipment this Aura enchants — `Creature.hasAttached(Any.attachedToBySource())`. The
 * intervening "if" (CR 603.4) asks whether such a creature exists, on anyone's side, and is
 * re-checked on resolution; the destroy hits exactly that one creature (an Equipment is attached to
 * at most one), never the Equipment. It is not targeted, so hexproof doesn't stop it.
 */
private val creatureCarryingEnchantedEquipment =
    GameObjectFilter.Creature.hasAttached(GameObjectFilter.Any.attachedToBySource())

val ArtificersHex = card("Artificer's Hex") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Equipment\n" +
        "At the beginning of your upkeep, if enchanted Equipment is attached to a creature, destroy " +
        "that creature."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.withSubtype("Equipment")))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.AnyPlayerControls(creatureCarryingEnchantedEquipment)
        effect = Effects.DestroyAll(creatureCarryingEnchantedEquipment)
        description = "At the beginning of your upkeep, if enchanted Equipment is attached to a " +
            "creature, destroy that creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Franz Vohwinkel"
        flavorText = "\"Some treasures aren't worth finding, let alone stealing.\"\n—Dack Fayden"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5cd9a1-da2e-44ef-9f2e-352dc9f92c50.jpg?1783939927"
    }
}
