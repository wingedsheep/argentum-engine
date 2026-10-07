package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val WorldBreaker = card("World Breaker") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 7
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, exile target artifact, enchantment, or land.\n" +
        "Reach\n" +
        "{2}{C}, Sacrifice a land: Return this card from your graveyard to your hand. " +
        "({C} represents colorless mana.)"

    keywords(Keyword.DEVOID, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val target = target(TargetFilter(GameObjectFilter.ArtifactEnchantmentOrLand))
        effect = Effects.Exile(target)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{C}"), Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        activateFromZone = Zone.GRAVEYARD
        description = "Return World Breaker from your graveyard to your hand"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "126"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0020a124-ba76-4d40-84e9-9803268d9f16.jpg?1783937903"
        ruling("2016-01-22", "You can activate the last ability only if World Breaker is in your graveyard.")
    }
}
