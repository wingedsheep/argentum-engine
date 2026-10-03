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
 * Colossal Dreadmask — Modern Horizons 3 #148 (common)
 * {4}{G}{G} · Artifact — Equipment
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +6/+6 and has trample.
 * Equip {3}{G}{G}
 *
 * Living weapon (CR 702.92) is composed as Drossclaw's create-then-attach enters trigger: the
 * token publishes to [CREATED_TOKENS] and [Effects.AttachEquipment] attaches this Equipment to it.
 */
val ColossalDreadmask = card("Colossal Dreadmask") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\nEquipped creature gets +6/+6 and has trample.\n" +
        "Equip {3}{G}{G}"

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
        ability = ModifyStats(6, 6, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    equipAbility("{3}{G}{G}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Caio Monteiro"
        flavorText = "No legs to quake, no lungs to bellow, but if you see its teeth, it's still too late."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98164430-64c1-465f-b786-45753c965f44.jpg?1783911264"
    }
}
