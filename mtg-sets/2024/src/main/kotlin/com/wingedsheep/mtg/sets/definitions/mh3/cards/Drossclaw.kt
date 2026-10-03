package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drossclaw — Modern Horizons 3 #89 (common)
 * {1}{B} · Artifact — Equipment
 *
 * Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ creature token,
 * then attach this to it.)
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, each opponent loses 1 life.
 * Equip {2}
 *
 * Living weapon (CR 702.92) is composed as the same create-then-attach enters trigger that
 * `jobSelect()` / `forMirrodin()` wire: the token publishes to [CREATED_TOKENS] and
 * [Effects.AttachEquipment] attaches this Equipment to it. The Germ is a 0/0 that survives as a
 * 1/1 only through the +1/+1 bonus.
 */
val Drossclaw = card("Drossclaw") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Living weapon (When this Equipment enters, create a 0/0 black Phyrexian Germ " +
        "creature token, then attach this to it.)\nEquipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, each opponent loses 1 life.\nEquip {2}"

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

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70e68656-3204-4bb5-9f31-8036083fcba6.jpg?1783911281"
    }
}
