package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

val RaidersKarve = card("Raiders' Karve") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Whenever this Vehicle attacks, look at the top card of your library. If it's a land card, you may put it onto the battlefield tapped.\nCrew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywordAbility(KeywordAbility.crew(3))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val land = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "You may put the land onto the battlefield tapped",
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Leave on top",
                showAllCards = true
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f572cbb1-49ee-4c95-90a3-82704cf92454.jpg?1783928184"
    }
}
