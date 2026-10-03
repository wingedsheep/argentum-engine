package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cranial Ram — Modern Horizons 3 #180 (common)
 * {B}{R} · Artifact — Equipment
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +X/+1, where X is the number of artifacts you control.
 * Equip {2}
 *
 * Living weapon is the same create-then-attach enters trigger as [Drossclaw]. The bonus is a
 * Layer 7c [GrantDynamicStats] recomputed at projection, so X tracks artifacts entering and
 * leaving (the Ram itself counts).
 */
val CranialRam = card("Cranial Ram") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\nEquipped creature gets +X/+1, where X is the " +
        "number of artifacts you control.\nEquip {2}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Germ"),
            imageUri = "https://cards.scryfall.io/normal/front/5/e/5ec719dc-6b07-4b1d-a79c-84ebced33422.jpg?1783911115"
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
        description = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian " +
            "Germ creature token, then attach this to it.)"
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmounts.fixed(1)
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Allen Williams"
        flavorText = "To ensure proper fit, the mandibles can be inserted directly into the ears."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62993aa2-4804-43cc-910a-c51e794f8508.jpg?1783911253"
    }
}
