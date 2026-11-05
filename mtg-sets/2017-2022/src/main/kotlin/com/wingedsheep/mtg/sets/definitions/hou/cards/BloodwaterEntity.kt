package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bloodwater Entity — Hour of Devastation #138 (canonical printing)
 * {1}{U}{R} · Creature — Elemental · 2/2
 *
 * Flying
 * Prowess
 * When this creature enters, you may put target instant or sorcery card from your graveyard
 * on top of your library.
 */
val BloodwaterEntity = card("Bloodwater Entity") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "Flying\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, you may put target instant or sorcery card from your graveyard on top of your library."

    keywords(Keyword.FLYING)
    prowess()

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Move(
            target = spellCard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Top
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/474d0a04-b640-4d1d-b538-2d946c1ff913.jpg?1783936011"
    }
}
