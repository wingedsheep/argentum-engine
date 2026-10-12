package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Iron-Craw Crusher
 * {7}
 * Artifact Creature — Wurm
 * 4/6
 * Prototype {2}{G}{G} — 2/5
 * Whenever this creature attacks, target attacking creature gets +X/+0 until end of turn, where X is
 * this creature's power.
 *
 * X is read on resolution from the Crusher's current power — 4 normally, 2 when prototyped, and any
 * pumps it has — and it may target itself, since it is an attacking creature.
 */
val IronCrawCrusher = card("Iron-Craw Crusher") {
    manaCost = "{7}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Wurm"
    power = 4
    toughness = 6
    oracleText = "Prototype {2}{G}{G} — 2/5 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Whenever this creature attacks, target attacking creature gets +X/+0 until end of turn, where X is this creature's power."

    keywordAbility(KeywordAbility.prototype("{2}{G}{G}", 2, 5))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val attacker = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.fixed(0), attacker)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64e2ec10-f45f-46ef-a076-52425bf3fe3b.jpg?1783920036"
    }
}
