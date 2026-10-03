package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mandibular Kite — Modern Horizons 3 #34 (common)
 * {W} · Artifact — Equipment
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +1/+1 and has flying.
 * Equip {3}{W}
 *
 * Living weapon (CR 702.92) is composed as the create-then-attach enters trigger, as on Drossclaw.
 */
val MandibularKite = card("Mandibular Kite") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\nEquipped creature gets +1/+1 and has flying.\n" +
        "Equip {3}{W}"

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
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    equipAbility("{3}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Bruno Biazotto"
        flavorText = "The Machine Orthodoxy finds skin impure, and will not sully their creations with impure material."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b922f71-18e6-4a74-b792-d477d4a1deca.jpg?1783911299"
    }
}
