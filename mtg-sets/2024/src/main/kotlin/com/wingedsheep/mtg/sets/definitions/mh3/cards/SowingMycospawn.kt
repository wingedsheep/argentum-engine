package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val SowingMycospawn = card("Sowing Mycospawn") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Fungus"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\nKicker {1}{C} (You may pay an additional {1}{C} as you cast this spell.)\nWhen you cast this spell, search your library for a land card, put it onto the battlefield, then shuffle.\nWhen you cast this spell, if it was kicked, exile target land."

    keywords(Keyword.DEVOID)
    keywordAbility(KeywordAbility.kicker("{1}{C}"))

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD
        )
    }
    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.WasKicked
        val land = target(TargetFilter.Land)
        effect = Effects.Exile(land)
    }

    metadata {
        ruling("2024-06-07", "Sowing Mycospawn's triggered abilities will resolve before Sowing Mycospawn does. If Sowing Mycospawn is countered or otherwise leaves the stack in response to those triggered abilities, the triggered abilities will still resolve as normal.")
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdfadb17-76ad-4d4d-9fa7-33c4b88b4c0a.jpg?1783911255"
    }
}
