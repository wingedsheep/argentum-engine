package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battery Bearer
 * {2}{G}{U}
 * Creature — Human Artificer
 * 3/4
 * Creatures you control have "{T}: Add {C}. This mana can't be spent to cast a nonartifact spell."
 * Whenever you cast an artifact spell with mana value 6 or greater, draw a card.
 */
val BatteryBearer = card("Battery Bearer") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 4
    oracleText = "Creatures you control have \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\"\n" +
        "Whenever you cast an artifact spell with mana value 6 or greater, draw a card."

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(
                    1,
                    restriction = ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT)),
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact.manaValueAtLeast(6))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "She always leads the charge."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb5306d6-0f08-429a-8590-1b8136f953a9.jpg?1783920032"
    }
}
