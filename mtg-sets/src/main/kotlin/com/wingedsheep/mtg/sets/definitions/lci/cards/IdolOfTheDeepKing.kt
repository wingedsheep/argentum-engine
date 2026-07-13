package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Idol of the Deep King // Sovereign's Macuahuitl (CR 702.167, The Lost Caverns of Ixalan)
 * {2}{R}
 * Artifact // Artifact — Equipment
 *
 * Front face — Idol of the Deep King ({2}{R}, Artifact)
 *   Flash
 *   When this artifact enters, it deals 2 damage to any target.
 *   Craft with artifact {2}{R} ({2}{R}, Exile this artifact, Exile another artifact you
 *   control or an artifact card from your graveyard: Return this card transformed under
 *   its owner's control. Craft only as a sorcery.)
 *
 * Back face — Sovereign's Macuahuitl (Artifact — Equipment)
 *   When this Equipment enters, attach it to target creature you control.
 *   Equipped creature gets +2/+0.
 *   Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)
 *
 * Implementation: the front face's ETB strike is [Triggers.EntersBattlefield] +
 * [Effects.DealDamage] at [Targets.Any] (damage source defaults to the trigger's source,
 * matching the "it deals" wording). The craft line uses the `craft(...)` helper with
 * [GameObjectFilter.Artifact] and `minCount = maxCount = 1` — "Craft with artifact" exiles
 * exactly one artifact material (CR 702.167a-b); resolution returns this card transformed
 * (back face) under its owner's control. The back face composes the standard Equipment
 * primitives: an ETB [Effects.AttachEquipment] trigger targeting a creature you control
 * (the Coral Sword / Malamet Scythe idiom), a no-filter [ModifyStats] static that defaults
 * to the equipped creature, and the canonical `equipAbility("{2}")` (sorcery-speed attach,
 * CR 702.6).
 */

private val IdolOfTheDeepKingFront = card("Idol of the Deep King") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "When this artifact enters, it deals 2 damage to any target.\n" +
        "Craft with artifact {2}{R} ({2}{R}, Exile this artifact, Exile another artifact you control or an artifact card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    keywords(Keyword.FLASH)

    // ETB: it deals 2 damage to any target.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(2, anyTarget)
    }

    // Craft with artifact {2}{R} — exactly one artifact material.
    craft(
        filter = GameObjectFilter.Artifact,
        cost = "{2}{R}",
        materialDescription = "artifact",
        minCount = 1,
        maxCount = 1
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Matteo Bassini"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1d8d8ef-c8b2-4e7c-89e4-b381dff20584.jpg?1782694484"
    }
}

private val SovereignsMacuahuitl = card("Sovereign's Macuahuitl") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+0.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // ETB: attach it to target creature you control.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature you control", TargetCreature(filter = TargetFilter.Creature.youControl()))
        effect = Effects.AttachEquipment(creature)
    }

    // Equipped creature gets +2/+0.
    staticAbility {
        ability = ModifyStats(2, 0)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Matteo Bassini"
        imageUri = "https://cards.scryfall.io/normal/back/d/1/d1d8d8ef-c8b2-4e7c-89e4-b381dff20584.jpg?1782694484"
    }
}

val IdolOfTheDeepKing: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = IdolOfTheDeepKingFront,
    backFace = SovereignsMacuahuitl
)
