package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Inga and Esika — March of the Machine #229
 * {2}{G}{U} · Legendary Creature — Human God 4/4
 *
 * Creatures you control have vigilance and "{T}: Add one mana of any color. Spend this mana only
 * to cast a creature spell."
 * Whenever you cast a creature spell, if three or more mana from creatures was spent to cast it,
 * draw a card.
 *
 * The intervening "if" is the cast predicate `PaidWithManaFromCardType(CREATURE, atLeast = 3)`:
 * the spell's payment can't change after it's cast, so checking it when the trigger fires is the
 * same as checking it again on resolution.
 */
val IngaAndEsika = card("Inga and Esika") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human God"
    oracleText = "Creatures you control have vigilance and \"{T}: Add one mana of any color. " +
        "Spend this mana only to cast a creature spell.\"\n" +
        "Whenever you cast a creature spell, if three or more mana from creatures was spent to " +
        "cast it, draw a card."
    power = 4
    toughness = 4

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaOfChoice(
                    restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.CREATURE)
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.Creature,
            requires = setOf(SpellCastPredicate.PaidWithManaFromCardType(CardType.CREATURE, atLeast = 3))
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aa97506-b943-4443-96b1-1b49f57d80aa.jpg?1783916948"
    }
}
