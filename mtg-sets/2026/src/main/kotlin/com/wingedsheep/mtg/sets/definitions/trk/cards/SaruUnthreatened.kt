package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Saru, Unthreatened
 * {2}{G}
 * Legendary Creature — Kelpien Officer
 * 3/2
 * Flash
 * When Saru enters, another target creature you control gains hexproof until end of turn.
 */
val SaruUnthreatened = card("Saru, Unthreatened") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Kelpien Officer"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "When Saru enters, another target creature you control gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.youControl().other())
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature)
        description = "Another target creature you control gains hexproof until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Ron Lemen"
        flavorText = "\"We all come from somewhere. We carry that place with us wherever we go.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/445de44d-8af9-4ea3-b165-9e8eb8939820.jpg?1793187139"
    }
}
