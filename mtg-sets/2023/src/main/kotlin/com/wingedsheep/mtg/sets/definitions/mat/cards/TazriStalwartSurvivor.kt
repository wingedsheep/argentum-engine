package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tazri, Stalwart Survivor — March of the Machine: The Aftermath #6
 * {2}{W} · Legendary Creature — Human Warrior · 3/3
 *
 * Each creature you control has "{T}: Add one mana of any of this creature's colors. Spend this
 * mana only to activate an ability of a creature. Activate only if this creature has another
 * activated ability."
 * {W}{U}{B}{R}{G}, {T}: Mill five cards. Put all creature cards with activated abilities that
 * aren't mana abilities from among the milled cards into your hand.
 *
 * "Another activated ability" counts whatever the creature currently has — printed, keyword
 * (cycling counts on the battlefield too) or granted — whether or not it can be activated; the
 * granted mana ability is itself one, so the gate is two or more. Its mana pays only abilities of
 * creatures on the battlefield, never a creature card's cycling or unearth.
 */
val TazriStalwartSurvivor = card("Tazri, Stalwart Survivor") {
    manaCost = "{2}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Each creature you control has \"{T}: Add one mana of any of this creature's colors. " +
        "Spend this mana only to activate an ability of a creature. Activate only if this creature has " +
        "another activated ability.\"\n" +
        "{W}{U}{B}{R}{G}, {T}: Mill five cards. Put all creature cards with activated abilities that " +
        "aren't mana abilities from among the milled cards into your hand."

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaOfChoice(
                    colorSet = ManaColorSet.ColorsOf(EffectTarget.Self),
                    restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                        CardType.CREATURE,
                        allowSpells = false,
                        allowAbilities = true,
                        permanentSourcesOnly = true,
                    ),
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                restrictions = listOf(
                    ActivationRestriction.OnlyIfCondition(Conditions.SourceHasAnotherActivatedAbility)
                ),
            ),
            filter = GroupFilter.AllCreaturesYouControl,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{U}{B}{R}{G}"), Costs.Tap)
        description = "Mill five cards. Put all creature cards with activated abilities that aren't " +
            "mana abilities from among the milled cards into your hand."
        effect = Effects.Pipeline {
            val milled = mill(5)
            val creatures = filter(
                milled,
                GameObjectFilter.Creature.withActivatedAbilities(excludeManaAbilities = true),
            )
            toHand(creatures)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/227d0d7d-544b-4736-914d-10d5d752eb42.jpg?1783916528"
        ruling("2023-05-12", "The mana produced by the ability granted by Tazri's first ability may be spent only to activate an ability of a creature on the battlefield. It can't be spent to activate abilities of creature cards in other zones.")
        ruling("2023-05-12", "The other activated ability the creature must have can be any activated ability, including one that can't be activated at the moment, or even one that can't be activated while the creature is on the battlefield. For example, you could tap a creature with cycling and Tazri's ability for mana.")
    }
}
