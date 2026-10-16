package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Puresteel Paladin — New Phyrexia #20
 * {W}{W} · Creature — Human Knight · 2 / 2
 *
 * Whenever an Equipment you control enters, you may draw a card.
 * Metalcraft — Equipment you control have equip {0} as long as you control three or more artifacts.
 *
 * "Have equip {0}" grants each Equipment a second equip ability ([GrantActivatedAbility]), exactly
 * as Astor, Bearer of Blades grants equip {1}; the printed equip ability stays. Metalcraft is an
 * ability word, so the grant is an ordinary conditional static gated by
 * [Conditions.YouControlAtLeast] and re-evaluated whenever legal actions are computed.
 */
val PuresteelPaladin = card("Puresteel Paladin") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Whenever an Equipment you control enters, you may draw a card.\n" +
        "Metalcraft — Equipment you control have equip {0} as long as you control three or more artifacts."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()).enters()
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever an Equipment you control enters, you may draw a card."
    }

    staticAbility {
        condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact)
        ability = GrantActivatedAbility(
            ability = ActivatedAbility.equip(ManaCost.parse("{0}")),
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca100248-fcd6-41ed-8d75-bcb473845edd.jpg?1783941324"
    }
}
