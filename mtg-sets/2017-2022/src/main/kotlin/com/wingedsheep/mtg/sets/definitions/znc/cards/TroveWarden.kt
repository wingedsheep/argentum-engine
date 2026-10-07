package com.wingedsheep.mtg.sets.definitions.znc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TroveWarden = card("Trove Warden") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Beast"
    power = 3
    toughness = 4
    oracleText = "Vigilance\n" +
        "Landfall — Whenever a land you control enters, exile target permanent card with mana value 3 or less from your graveyard.\n" +
        "When this creature dies, put each permanent card exiled with it onto the battlefield under the control of that card's owner."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val permanent = target(TargetFilter.PermanentInYourGraveyard.manaValueAtMost(3))
        effect = Effects.ExileLinkedToSource(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3336593c-c83c-48e7-9173-2c2b74b94d3b.jpg?1783929495"
    }
}
