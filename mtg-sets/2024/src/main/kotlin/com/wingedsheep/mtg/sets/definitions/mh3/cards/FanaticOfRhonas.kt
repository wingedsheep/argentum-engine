package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Fanatic of Rhonas
 * {1}{G}
 * Creature — Snake Druid
 * 1/4
 *
 * {T}: Add {G}.
 * Ferocious — {T}: Add {G}{G}{G}{G}. Activate only if you control a creature with power 4 or greater.
 * Eternalize {2}{G}{G} ({2}{G}{G}, Exile this card from your graveyard: Create a token that's a copy
 * of it, except it's a 4/4 black Zombie Snake Druid with no mana cost. Eternalize only as a sorcery.)
 *
 * Eternalize (CR 702.129) is the shared `eternalize(cost)` helper — `eternalizeAbility(cost)` builds the
 * sorcery-speed graveyard ability that exiles the card as a cost and makes a 4/4 black Zombie token copy
 * with no mana cost, marked `Keyword.ETERNALIZE` so "whenever you activate an eternalize ability" sees it.
 */
val FanaticOfRhonas = card("Fanatic of Rhonas") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Snake Druid"
    power = 1
    toughness = 4
    oracleText = "{T}: Add {G}.\n" +
        "Ferocious — {T}: Add {G}{G}{G}{G}. Activate only if you control a creature with power 4 or greater.\n" +
        "Eternalize {2}{G}{G} ({2}{G}{G}, Exile this card from your graveyard: Create a token that's a copy " +
        "of it, except it's a 4/4 black Zombie Snake Druid with no mana cost. Eternalize only as a sorcery.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 4)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
            )
        )
    }

    eternalize("{2}{G}{G}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f9fb33a-3b39-4aff-93b8-aedafe0ea694.jpg?1783911261"
    }
}
