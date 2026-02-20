package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Psychic Trance
 * {2}{U}{U}
 * Instant
 * Until end of turn, Wizards you control gain "{T}: Counter target spell."
 */
val PsychicTrance = card("Psychic Trance") {
    manaCost = "{2}{U}{U}"
    typeLine = "Instant"
    oracleText = "Until end of turn, Wizards you control gain \"{T}: Counter target spell.\""

    spell {
        effect = GrantActivatedAbilityToGroupEffect(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = CounterSpellEffect,
                targetRequirement = Targets.Spell
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Wizard").youControl()),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Rebecca Guay"
        flavorText = "The Riptide Project may be the only school devoted to preventing the spread of knowledge."
        imageUri = "https://cards.scryfall.io/large/front/d/5/d5e55695-16cc-4373-8078-959f1ded4c6d.jpg?1562945989"
    }
}
